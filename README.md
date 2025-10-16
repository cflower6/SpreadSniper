# SpreadSniper


[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Deploy on Railway](https://img.shields.io/badge/Deploy-Railway-black?logo=railway)](https://railway.app/template)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/cflower6/SpreadSniper/pulls)

---

**A Kotlin-based arbitrage and spread-monitoring bot (work-in-progress)**

> This project is a Kotlin Gradle service designed to detect and act on arbitrage opportunities across on-chain liquidity sources. It’s still under development and uses Railway for deployment.

---

## 🧭 Table of Contents
- [What it is](#what-it-is)
- [Status](#status)
- [Features](#features)
- [Repository Structure](#repository-structure)
- [Prerequisites](#prerequisites)
- [Quickstart](#quickstart)
- [Configuration](#configuration)
- [Running](#running)
- [Architecture Notes](#architecture-notes)
- [Testing](#testing)
- [Deployment](#deployment)
- [TODOs](#todos)
- [Contributing](#contributing)

---

## 💡 What it is
SpreadSniper is a Kotlin-based service that monitors token price spreads across multiple decentralized exchanges (DEXs) and computes potential arbitrage opportunities. It’s built for modularity, safety, and scalability with Kotlin + Gradle.

---

## 🚧 Status
This repository is currently **WIP (Work in Progress)** — core monitoring logic is being developed, and execution modules are under testing.

---

## ⚙️ Features
- ✅ Monitor real-time token price spreads across RPC endpoints  
- ✅ Estimate profits accounting for gas, slippage, and fees  
- 🧩 Modular architecture for multiple DEX adapters  
- 🚀 Deployable via [Railway](https://railway.app)  
- 🔒 Secure configuration through environment variables  

---

## 📁 Repository Structure

SpreadSniper/
├── .idea/ # IntelliJ project config
├── gradle/ # Gradle wrapper
├── build.gradle.kts # Kotlin build configuration
├── settings.gradle.kts
├── railway.json # Railway deployment config
└── src/
└── main/
└── kotlin/ # Main Kotlin source files

---

## 🧰 Prerequisites
- **JDK 17+**
- **Gradle 8+** (included via wrapper)
- Access to a valid **EVM RPC provider** (Alchemy, Infura, etc.)
- (Optional) A funded wallet private key for on-chain execution  

---

## ⚡ Quickstart

```bash
# Clone the repo
git clone https://github.com/cflower6/SpreadSniper.git
cd SpreadSniper

# Build
./gradlew clean build

# Run (if main class is defined)
./gradlew run
```

If you prefer running manually:
java -jar build/libs/<your-artifact>.jar

# RPC / Network
RPC_URL=https://eth-mainnet.alchemyapi.io/v2/YOUR_KEY
NETWORK=mainnet

# Wallet
PRIVATE_KEY=0xyourprivatekeyhere

# Bot Settings

MIN_PROFIT_USD=5.00

SLIPPAGE_TOLERANCE=0.005

GAS_MULTIPLIER=1.1

# Contracts / Tokens

TOKEN_A_ADDRESS=0x...

TOKEN_B_ADDRESS=0x...

DEX_ROUTER_ADDRESS=0x...

# ▶️ Running

Dry Run Mode (recommended for testing):

DRY_RUN=true ./gradlew run


Full Run (production):

./gradlew run


Alternative testnets:
Set NETWORK=sepolia or another testnet in your .env.

# 🧠 Architecture Notes

Modular fetchers for each DEX or RPC source

Parallelized price polling with safe concurrency and retries

Profit calculation includes gas and slippage

Configurable circuit breaker for volatile gas prices

# 🧪 Testing

Add tests under src/test/kotlin and run:

./gradlew test

# 🚀 Deployment

Railway deployment supported via railway.json:

railway up

Make sure to set environment variables through the Railway dashboard before deployment.

# 📝 TODOs

 Define main entrypoint and CLI flags

 Add unit & integration tests

 Add logging + metrics

 Document environment variable schema

 Add LICENSE and CONTRIBUTING guidelines

# 🤝 Contributing

Fork the repo

Create your branch (git checkout -b feature/amazing-feature)

Commit changes (git commit -m 'Add amazing feature')

Push to the branch (git push origin feature/amazing-feature)

Open a Pull Request

# 💬 Feedback

If you find this project interesting, drop a star ⭐ and feel free to open issues or PRs for improvements!

---
