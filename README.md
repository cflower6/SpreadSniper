# SpreadSniper

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
- [License](#license)

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

