# Behavioral Biometrics & Insider Threat Detection System 🛡️

A production-grade Java application that uses **Keystroke Dynamics** to identify unauthorized users. This tool establishes a behavioral baseline for a user's typing rhythm and triggers security alerts if an anomaly is detected, simulating an **Insider Threat Detection** scenario.

## 🎯 Project Overview
In cybersecurity, behavioral biometrics focuses on *how* you type rather than *what* you type. This system captures:
* **Dwell Time:** The duration a key is held down.
* **Flight Time:** The interval between consecutive keystrokes.
* **Typing Speed:** Real-time WPM (Words Per Minute) and KPS (Keys Per Second).

## ✨ Key Features
* **Real-Time Security Dashboard:** Built with Java Swing, featuring a dynamic Risk Status Panel.
* **Adaptive Baseline:** A 15-second calibration phase to "learn" the authorized user's rhythm.
* **Automated Forensics:** Generates a `security_breach_report.log` automatically upon detecting high-variance typing (>35%).
* **Multi-Threaded Engine:** Analysis runs on a background thread to ensure zero UI lag.

## 🏗️ OOP Architecture
This project strictly adheres to core Object-Oriented principles:
* **Abstraction:** Utilizes the `AnalyticTask` interface for modular security checks.
* **Inheritance:** `BaseMonitor` serves as the parent for specialized `SpeedAnalyzer` and `PatternAnalyzer` classes.
* **Polymorphism:** A `List<AnalyticTask>` handles multiple analysis engines simultaneously.
* **Encapsulation:** Biometric data is stored in private fields with secure accessors to prevent tampering.

## 🚀 Execution Guide

### Prerequisites
* Java JDK 11 or higher installed.
* Git (optional for cloning).

### 1. Compile the Source
Open your terminal/command prompt in the project folder and run:
```bash
javac -encoding UTF-8 *.java
