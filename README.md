<div align="center">



<br>

<img src="assets/logo.png" width="180"/>


### Chat Beyond the Internet

*Modern Offline Messaging Platform*

<p>

<a href="https://play.google.com/store/apps/details?id=com.aistudio.vozo.kdnoqm">
<img src="https://img.shields.io/badge/Google_Play-34A853?style=for-the-badge&logo=googleplay&logoColor=white"/>
</a>

<a href="https://www.voikestechnologies.com/">
<img src="https://img.shields.io/badge/VOIKES_Technologies-6C63FF?style=for-the-badge"/>
</a>

<img src="https://img.shields.io/github/stars/ananvaypandey/VOZO?style=for-the-badge"/>

<img src="https://img.shields.io/github/forks/ananvaypandey/VOZO?style=for-the-badge"/>

</p>

</div>

---

# Phase 05 — Phone ↔ Desktop Mesh (in development)

VOZO is being rebuilt as a **Kotlin Multiplatform (Compose Multiplatform)** app
targeting Android (Play Store) + Windows + macOS. Offline-first, P2P mesh.

> Legacy Google AI Studio prototype content below is retained for reference
> and will be replaced as development proceeds.

## Current status (P05)
- **Two phones can talk with no internet** (P03). Peers are found and connected
  over Bluetooth / Wi-Fi Direct using Google Nearby Connections.
- **Your phone and your computer can now talk too.** The phone hosts a TCP
  endpoint on port `47653` and advertises it; the desktop dials in.
- Messages are written to the local database first, then sent to the peer.
- The **Mesh** tab shows live discovered peers and their connection state.
  Tap a peer to open a direct chat.
- The phone's Mesh tab lists the addresses a computer can reach it on, so you
  can copy one straight into the desktop app.
- End-to-end encryption is deliberately deferred until the mesh is stable.

## How to test phone ↔ computer
1. Install `vozo-debug.apk` on your phone (Android 6+, Google Play Services).
2. Turn on **mobile hotspot** on the phone, then connect the computer to it.
3. On the phone, open the **Mesh** tab. Copy one of the listed
   `192.168.x.x:47653` addresses (use the hotspot one).
4. Run the desktop app (`./gradlew :vozo:run` from `app/`, or use the packaged
   Windows build). Open its **Mesh** tab and paste that address into
   **Phone address**, then press **Connect**.
5. Tap the peer and send a message. It should appear on the phone instantly.
6. Keep mobile data off to confirm the link is genuinely local.

If it will not connect, the usual causes are a firewall blocking inbound
connections on the computer, or picking a `10.x`/`192.168.x` address that
belongs to a different interface than the one the hotspot uses.

## How to test with two phones
1. Install `vozo-debug.apk` on **both** phones (Android 6+, Google Play Services).
2. Launch VOZO on both and allow the Nearby / Bluetooth / Location permissions.
3. Open the **Mesh** tab on both. Each phone should list the other.
4. Tap the peer, type a message, send. It should appear on the other phone.
5. Turn off Wi-Fi and mobile data on both to confirm it is truly offline.

If the Mesh tab stays empty, check that Location is enabled (Android requires
it for peer discovery) and that both phones are within a few metres.

## Project layout
```
app/
  vozo/
    src/commonMain/       shared UI + data (theme, nav, screens, ChatStore, mesh)
    src/commonMain/sqldelight/   SQLite schema
    src/androidMain/      Android entry, Nearby + TCP-host transports, SQLite driver
    src/desktopMain/      desktop entry, TCP client transport, SQLite driver
    src/desktopTest/      loopback tests for framing, handshake and delivery
  gradle/libs.versions.toml
```

## Build
```bash
cd app
./gradlew :vozo:assembleDebug              # Android APK
./gradlew :vozo:desktopTest                # transport tests
./gradlew :vozo:packageMsi/packageExe      # Windows installer
./gradlew :vozo:run                        # run desktop app
```

Requires JDK 17+ and Android SDK (see `app/local.properties`).

---

# 🌍 What is VOZO?

VOZO is a modern **offline-first messaging application** that allows users to communicate **without mobile data, internet, SIM cards, or traditional messaging infrastructure.**

Built using **Google Nearby Connections API**, VOZO enables seamless peer-to-peer communications over different alternatives as BLUETOOH and WIFI

---

# ✨ Features

- 💬 Offline Messaging
- 📷 Image Sharing
- 📁 File Transfer
- 👥 Create Groups
- 📍 Nearby Device Discovery
- ⚡ Fast Peer-to-Peer Connections
- 🔒 Secure Local Communication
- 🌐 No Internet Required
- 📡 Bluetooth Connectivity
- 📶 Wi-Fi Direct Support

---

# 🎬 Demo

<p align="center">

<img src="assets/demo.gif" width="900"/>

</p>

---

# 📱 Screenshots

| Home | Chats |
|------|-------|
| <img src="assets/home.png" width="300"/> | <img src="assets/chat.png" width="300"/> |

| Nearby Devices | Settings |
|------|------|
| <img src="assets/discover.png" width="300"/> | <img src="assets/settings.png" width="300"/> |

---

# ⚙️ Tech Stack

<div align="center">

<img src="https://skillicons.dev/icons?i=kotlin,androidstudio,firebase,git,github"/>

</div>

---

# 🏗 Architecture

```text
Nearby Devices
       │
       ▼
Device Discovery
       │
       ▼
Secure Connection
       │
       ▼
Message Transfer
       │
       ▼
Images & Files
       │
       ▼
Delivered Offline
```

---

# 🚀 Core Technologies

| Technology | Usage |
|------------|------|
| Nearby Connections API | Device Discovery |
| Bluetooth | Offline Communication |
| Wi-Fi Direct | High-Speed Transfer |
| Android | Native Platform |
| Kotlin | App Development |
| Material Design 3 | UI |

---

# 🔥 Why VOZO?

✅ Works without Internet

✅ No SIM Required

✅ Lightning Fast

✅ Secure Local Messaging

✅ Easy Device Discovery

✅ Beautiful Material UI

---

# 🎯 Use Cases

🏫 College Campuses

🏕 Camping

🚑 Emergency Communication

🎉 Festivals

🏢 Offices

🌍 Rural Areas

Military Exercises

Disaster Recovery

---

# 📈 Roadmap

- [x] Offline Messaging
- [x] Nearby Discovery
- [x] Bluetooth Connections
- [x] Wi-Fi Direct
- [x] Image Sharing
- [x] File Sharing
- [ ] Voice Messages
- [ ] Video Sharing
- [ ] Offline Calling
- [x] Mesh Networking
- [x] Cross Platform Support

---

# 📊 Repository Stats

<p align="center">

<img src="https://github-readme-stats.vercel.app/api/pin/?username=ananvaypandey&repo=VOZO&theme=tokyonight"/>

</p>

---

# 🌟 Powered By

<div align="center">

<img src="https://img.shields.io/badge/Nearby_Connections_API-4285F4?style=for-the-badge"/>

<img src="https://img.shields.io/badge/Bluetooth-0082FC?style=for-the-badge"/>

<img src="https://img.shields.io/badge/WiFi_Direct-10B981?style=for-the-badge"/>

<img src="https://img.shields.io/badge/Material_Design_3-6200EE?style=for-the-badge"/>

</div>

---

# ❤️ Developed By

<div align="center">

<img src="assets/voikes-logo.png" width="140"/>

## VOIKES Technologies Pvt. Ltd.

### Building Human-Centered Technology

Made with ❤️ in India 🇮🇳

</div>

---

<div align="center">

<img src="https://quotes-github-readme.vercel.app/api?type=horizontal&theme=tokyonight"/>

<br>

<img src="https://capsule-render.vercel.app/api?type=waving&height=140&section=footer&color=0:22D3EE,50:6C63FF,100:00C2FF"/>

</div>
