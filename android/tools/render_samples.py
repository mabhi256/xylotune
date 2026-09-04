#!/usr/bin/env python3
"""Renders the 16 pre-baked xylophone bar samples (8 notes x {metal, wood}) that the
Android app's native mixer plays back through Oboe.

This is a from-scratch reimplementation of the modal-synthesis math in the web app's
SOUND_PROFILES / playNote (see ../../index.html) using nothing but the standard library,
so the Android instrument is sonically faithful to the web one without pulling the Web
Audio API (or any audio library) into a build tool. Every constant below is copied
verbatim from index.html; if that file's SOUND_PROFILES ever changes, re-copy the
numbers here and re-run.

One deliberate, permanent divergence from the web: index.html re-randomizes each note's
fundamental frequency by +/-0.1% on every strike (a "live" instrument touch). A
pre-rendered sample is baked once and replayed identically every time, so that per-strike
detuning is dropped here -- not a bug, just what "pre-rendered" means.

Usage:
    python render_samples.py

Writes 16 mono 16-bit PCM WAVs into ../app/src/main/assets/samples/.
"""

import math
import random
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 48_000

# ---- ported verbatim from index.html's NOTES (name, freq) ----
NOTES = [
    ("C", 523.25),
    ("D", 587.33),
    ("E", 659.26),
    ("F", 698.46),
    ("G", 783.99),
    ("A", 880.00),
    ("B", 987.77),
    ("C2", 1046.5),
]

# ---- ported verbatim from index.html's SOUND_PROFILES ----
SOUND_PROFILES = {
    "metal": {
        "modes": [
            {"ratio": 1.000, "gain": 0.55, "decay": 1.5},
            {"ratio": 1.004, "gain": 0.28, "decay": 1.3},
            {"ratio": 2.756, "gain": 0.20, "decay": 0.60},
            {"ratio": 5.404, "gain": 0.11, "decay": 0.30},
            {"ratio": 8.933, "gain": 0.05, "decay": 0.16},
        ],
        "noise": {"freq_mul": 2.5, "gain": 0.22, "decay": 0.012},
        "lowpass": None,
    },
    "wood": {
        "modes": [
            {"ratio": 1.000, "gain": 0.70, "decay": 0.35},
            {"ratio": 3.932, "gain": 0.16, "decay": 0.20},
            {"ratio": 9.538, "gain": 0.05, "decay": 0.09},
        ],
        "noise": {"freq_mul": 1.1, "gain": 0.40, "decay": 0.02},
        "lowpass": 3400,
    },
}

ATTACK_SEC = 0.0015  # matches playNote's g.gain.exponentialRampToValueAtTime(m.gain, t0 + 0.0015)
FLOOR = 0.0001  # matches playNote's 0.0001 ramp floor / setTargetAtTime target
TAIL_SEC = 0.3  # matches playNote's osc.stop(t0 + m.decay + 0.3)
NOISE_STOP_PAD_SEC = 0.02  # matches playNote's noise.stop(t0 + noise.decay + 0.02)

# getNoiseBuffer(): a single 30ms white-noise burst with a linear amplitude decay,
# generated once and reused (same realization) for every note and every material -- exactly
# as the web memoizes one noiseBuffer for the whole page's lifetime.
NOISE_BUFFER_SEC = 0.03
_rng = random.Random(1234567)  # fixed seed: reproducible output across re-runs
_noise_len = int(SAMPLE_RATE * NOISE_BUFFER_SEC)
NOISE_BUFFER = [
    (_rng.random() * 2 - 1) * (1 - i / _noise_len) for i in range(_noise_len)
]


def biquad_normalized(b0, b1, b2, a0, a1, a2):
    return (b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)


def biquad_lowpass(f0, q, fs):
    """RBJ Audio-EQ-Cookbook lowpass -- the exact formula Web Audio's BiquadFilterNode
    (type='lowpass') implements internally."""
    w0 = 2 * math.pi * f0 / fs
    cos_w0, sin_w0 = math.cos(w0), math.sin(w0)
    alpha = sin_w0 / (2 * q)
    b0 = (1 - cos_w0) / 2
    b1 = 1 - cos_w0
    b2 = (1 - cos_w0) / 2
    a0 = 1 + alpha
    a1 = -2 * cos_w0
    a2 = 1 - alpha
    return biquad_normalized(b0, b1, b2, a0, a1, a2)


def biquad_bandpass(f0, q, fs):
    """RBJ "constant skirt gain, peak gain = Q" bandpass -- what Web Audio's
    BiquadFilterNode (type='bandpass') implements internally."""
    w0 = 2 * math.pi * f0 / fs
    cos_w0, sin_w0 = math.cos(w0), math.sin(w0)
    alpha = sin_w0 / (2 * q)
    b0 = sin_w0 / 2
    b1 = 0.0
    b2 = -sin_w0 / 2
    a0 = 1 + alpha
    a1 = -2 * cos_w0
    a2 = 1 - alpha
    return biquad_normalized(b0, b1, b2, a0, a1, a2)


def apply_biquad(samples, coeffs):
    """Direct Form I, zero initial conditions -- one IIR pass over the buffer."""
    b0, b1, b2, a1, a2 = coeffs
    out = [0.0] * len(samples)
    x1 = x2 = y1 = y2 = 0.0
    for i, x0 in enumerate(samples):
        y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        out[i] = y0
        x2, x1 = x1, x0
        y2, y1 = y1, y0
    return out


def mode_amplitude(t, gain, decay):
    """Reproduces g.gain's automation: exponential ramp 0.0001 -> m.gain over
    [0, ATTACK_SEC], then an asymptotic (setTargetAtTime) decay toward 0.0001 with time
    constant decay/6."""
    if t < ATTACK_SEC:
        return FLOOR * (gain / FLOOR) ** (t / ATTACK_SEC)
    tau = decay / 6
    return FLOOR + (gain - FLOOR) * math.exp(-(t - ATTACK_SEC) / tau)


def render_modes(freq, modes, total_len):
    out = [0.0] * total_len
    for m in modes:
        gain, decay, ratio = m["gain"], m["decay"], m["ratio"]
        stop_at = decay + TAIL_SEC
        omega = 2 * math.pi * freq * ratio / SAMPLE_RATE
        for i in range(total_len):
            t = i / SAMPLE_RATE
            if t >= stop_at:
                break
            amp = mode_amplitude(t, gain, decay)
            out[i] += amp * math.sin(omega * i)
    return out


def render_noise(freq, noise_params, total_len):
    gain, decay, freq_mul = noise_params["gain"], noise_params["decay"], noise_params["freq_mul"]
    raw = [0.0] * total_len
    # exponentialRampToValueAtTime(FLOOR, t0+decay) starting from `gain` at t=0, then the
    # AudioParam holds its last value (FLOOR) until the source itself runs out of buffer.
    n = min(len(NOISE_BUFFER), total_len)
    for i in range(n):
        t = i / SAMPLE_RATE
        if t < decay:
            amp = gain * (FLOOR / gain) ** (t / decay)
        else:
            amp = FLOOR
        raw[i] = NOISE_BUFFER[i] * amp
    coeffs = biquad_bandpass(freq * freq_mul, 1.0, SAMPLE_RATE)
    return apply_biquad(raw, coeffs)


def render_note(freq, profile):
    modes = profile["modes"]
    total_sec = max(m["decay"] + TAIL_SEC for m in modes)
    total_len = math.ceil(SAMPLE_RATE * total_sec)

    signal = render_modes(freq, modes, total_len)
    noise = render_noise(freq, profile["noise"], total_len)
    for i in range(total_len):
        signal[i] += noise[i]

    if profile["lowpass"]:
        coeffs = biquad_lowpass(profile["lowpass"], 1.0, SAMPLE_RATE)
        signal = apply_biquad(signal, coeffs)

    return signal


def write_wav(path, samples):
    clamped = [max(-32768, min(32767, int(round(s * 32767)))) for s in samples]
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(struct.pack(f"<{len(clamped)}h", *clamped))


def main():
    out_dir = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "samples"
    out_dir.mkdir(parents=True, exist_ok=True)

    for material, profile in SOUND_PROFILES.items():
        rendered = {}
        for name, freq in NOTES:
            print(f"Rendering {material}_{name}...")
            rendered[name] = render_note(freq, profile)

        # One shared headroom scale per material (not per note): the mode-gain profile is
        # identical across a material's 8 notes, so scaling each note by its own peak would
        # equalize loudness the web never intended. Scale by the loudest note instead.
        peak = max(abs(s) for buf in rendered.values() for s in buf)
        scale = min(1.0, 0.98 / peak) if peak > 0 else 1.0

        for name, buf in rendered.items():
            scaled = [s * scale for s in buf]
            write_wav(out_dir / f"{material}_{name}.wav", scaled)

    print(f"Wrote 16 samples to {out_dir}")


if __name__ == "__main__":
    main()
