<div align="center">

# FlickTrove — Premium Cinema & TV Show Tracker for Android

<img src="docs/assets/Icon.svg" alt="FlickTrove Logo - Android Cinema & TV Tracker" width="100" height="100" />
<img src="https://capsule-render.vercel.app/api?type=venom&color=2DD4BF&height=180&section=header&text=FlickTrove&fontSize=80&fontAlignY=40&fontColor=ffffff" alt="FlickTrove Banner" width="100%" />

<p align="center">
  <a href="https://alle-0.github.io/FlickTrove/"><img src="https://img.shields.io/badge/Website-Live_Showcase-00F2FE?style=for-the-badge&logo=googlechrome&logoColor=black" alt="Official Website"></a>
  <a href="https://github.com/Alle-0/FlickTrove/releases/latest"><img src="https://img.shields.io/github/v/release/Alle-0/FlickTrove?label=Latest%20Release&style=for-the-badge&color=2DD4BF&logo=github" alt="Latest Release"></a>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Firebase-Integrated-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase">
</p>

> **The ultimate personal cinema and TV show companion for Android.**
>
> *FlickTrove combines a unique glassmorphic interface, dynamic color palettes, and fluid animations with the full **TMDB catalog**, smart release notifications, and offline-first tracking built in **Kotlin** & **Jetpack Compose**.*

</div>

---

<details open>
  <summary><b><img src="docs/assets/readme_icons/lista.svg" width="20" height="20" align="absmiddle" /> Table of Contents</b></summary>
  <ol>
    <li><a href="#the-project"><img src="docs/assets/readme_icons/ciak.svg" width="18" height="18" align="absmiddle" /> The Project</a></li>
    <li><a href="#download--installation"><img src="docs/assets/readme_icons/scaricare.svg" width="18" height="18" align="absmiddle" /> Download & Installation</a></li>
    <li><a href="#key-features"><img src="docs/assets/readme_icons/sparkle.svg" width="18" height="18" align="absmiddle" /> Key Features</a></li>
    <li><a href="#ui-screenshots"><img src="docs/assets/readme_icons/smartphone_magia.svg" width="18" height="18" align="absmiddle" /> UI Screenshots</a></li>
    <li><a href="#architecture--technology"><img src="docs/assets/readme_icons/settings.svg" width="18" height="18" align="absmiddle" /> Architecture & Technology</a></li>
    <li><a href="#design--ui"><img src="docs/assets/readme_icons/palette.svg" width="18" height="18" align="absmiddle" /> Design & UI</a></li>
    <li><a href="#developer-note"><img src="docs/assets/readme_icons/scudo_privacy.svg" width="18" height="18" align="absmiddle" /> Developer Note</a></li>
    <li><a href="#usage"><img src="docs/assets/readme_icons/play.svg" width="18" height="18" align="absmiddle" /> Usage</a></li>
    <li><a href="#support"><img src="docs/assets/readme_icons/heart.svg" width="18" height="18" align="absmiddle" /> Support</a></li>
    <li><a href="#license"><img src="docs/assets/readme_icons/documento.svg" width="18" height="18" align="absmiddle" /> License</a></li>
    <li><a href="#contacts--credits"><img src="docs/assets/readme_icons/people.svg" width="18" height="18" align="absmiddle" /> Contacts & Credits</a></li>
  </ol>
</details>

---

## <h2 id="the-project"><img src="docs/assets/readme_icons/ciak.svg" width="24" height="24" align="absmiddle" /> The Project</h2>

**What is the motivation behind FlickTrove?**
There are many apps for tracking movies and TV shows, but they often lack visual polish or feel sluggish. The goal of FlickTrove is to provide enthusiasts with not just a useful tool, but a premium, responsive, and delightful experience, fully utilizing modern Android technologies.

This project solves the need for an elegant personal library, cloud-synchronized, with timely notifications for new releases.

---

## <h2 id="download--installation"><img src="docs/assets/readme_icons/scaricare.svg" width="24" height="24" align="absmiddle" /> Download & Installation</h2>

Experience FlickTrove on your Android device right now. Download the latest compiled APK directly from our releases page:

<br>

<div align="center">
  <a href="https://github.com/Alle-0/FlickTrove/releases/latest">
    <img src="https://img.shields.io/badge/Download_APK-Latest_Release-2DD4BF?style=for-the-badge&logo=android&logoColor=black" alt="Download APK" />
  </a>
</div>

<br>

> **Note:** Since the app is not currently distributed via the Google Play Store, you will need to allow **"Install from unknown sources"** in your Android security settings before installing the downloaded APK file.

---

## <h2 id="key-feature"><img src="docs/assets/readme_icons/sparkle.svg" width="24" height="24" align="absmiddle" /> Key Features</h2>

FlickTrove is not just a tracker, it's a personal library built tailored for enthusiasts:

- 🔍 **Comprehensive Search**: Access the entire TMDB catalog. Find movies, TV shows, and explore biographical details and filmographies of actors.
- 📁 **Custom Organization**: Create custom folders. Assign names, dynamic colors via a Color Wheel, and organize your watchlists the way you prefer.
- 🔔 **Smart Notifications**: Never miss a release. Receive timely alerts when a movie or TV show episode you are waiting for is released.
- 📊 **Advanced Statistics**: Monitor your watch time, analyze your favorite genres, and see how much of your life you've dedicated to cinema and TV series.
- 🎬 **Episodic Tracking**: Keep track of which episodes you've already watched. Filter by seasons and always stay up to date with your favorite series.
- ☁️ **Cloud Sync**: Native support for Firebase to save your data (accounts & backups).
- 📴 **Offline-First**: Access your personal library and save your preferences even without an internet connection, thanks to solid local caching (Room DB).
- 🌍 **Localization**: Native multi-language architecture for an international experience (currently English and Italian).
- 🍿 **Streaming Providers**: Discover exactly where to stream your favorite movies and shows.
- 📅 **Updates Zone**: A dedicated feed to keep track of upcoming episodes, theatrical releases, and latest news.
- 🔄 **Universal Data Import**: Smart migration engine that recognizes and imports exports from **Letterboxd**, **IMDb**, **Trakt.tv**, **TVTime**, **Serializd**, and any custom **CSV/JSON** format.

---

## <h2 id="ui-screenshots"><img src="docs/assets/readme_icons/smartphone_magia.svg" width="24" height="24" align="absmiddle" /> UI Screenshots</h2>

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Home & Blur Effect</b></td>
      <td align="center"><b>Recommendations</b></td>
      <td align="center"><b>Immersive Details</b></td>
    </tr>
    <tr>
      <td align="center">&nbsp;&nbsp;<img src="docs/assets/screenshot_home.webp" width="240"/>&nbsp;&nbsp;</td>
      <td align="center">&nbsp;&nbsp;<img src="docs/assets/screenshot_explore.webp" width="240"/>&nbsp;&nbsp;</td>
      <td align="center">&nbsp;&nbsp;<img src="docs/assets/screenshot_dune.webp" width="240"/>&nbsp;&nbsp;</td>
    </tr>
    <tr>
      <td align="center"><br><b>Advanced Statistics</b></td>
      <td align="center"><br><b>Custom Folders</b></td>
      <td align="center"><br><b>Deep Details</b></td>
    </tr>
    <tr>
      <td align="center">&nbsp;&nbsp;<img src="docs/assets/screenshot_stats.webp" width="240"/>&nbsp;&nbsp;</td>
      <td align="center">&nbsp;&nbsp;<img src="docs/assets/screenshot_folders.webp" width="240"/>&nbsp;&nbsp;</td>
      <td align="center">&nbsp;&nbsp;<img src="docs/assets/screenshot_detail.webp" width="240"/>&nbsp;&nbsp;</td>
    </tr>
  </table>
</div>

---

## <h2 id="architecture--technology"><img src="docs/assets/readme_icons/settings.svg" width="24" height="24" align="absmiddle" /> Architecture & Technology</h2>

Behind a gorgeous interface lies a solid and scalable engine. We used the best practices of modern Android development.

| Category | Stack / Library |
| :--- | :--- |
| **Language** | <img src="https://img.shields.io/badge/100%25_Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"> |
| **Architecture** | <img src="https://img.shields.io/badge/MVVM-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="MVVM"> |
| **UI Framework** | <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose"> <img src="https://img.shields.io/badge/Material_3-EADDFF?style=for-the-badge&logo=materialdesign&logoColor=black" alt="Material 3"> |
| **Networking** | <img src="https://img.shields.io/badge/Retrofit_2-FF6347?style=for-the-badge&logo=square&logoColor=white" alt="Retrofit"> <img src="https://img.shields.io/badge/OkHttp-109D59?style=for-the-badge&logo=square&logoColor=white" alt="OkHttp"> |
| **Local Database** | <img src="https://img.shields.io/badge/Room_DB-3DDC84?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room"> |
| **Dependency Injection** | <img src="https://img.shields.io/badge/Dagger_Hilt-000000?style=for-the-badge&logo=google&logoColor=white" alt="Hilt"> |
| **Images & Colors** | <img src="https://img.shields.io/badge/Coil-2A2A2A?style=for-the-badge&logo=android&logoColor=white" alt="Coil"> (Loading & Dynamic Color Extraction) |
| **Backend & Auth** | <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase"> |

<details>
<summary><b><img src="docs/assets/readme_icons/2_server.svg" width="18" height="18" align="absmiddle" /> Technical Deep Dive</b></summary>
<br>
FlickTrove adopts the most modern Android patterns: Kotlin Coroutines and Flows for reactive data management. Hilt simplifies dependencies, making the code testable and modular. Navigation is handled via the natively integrated Compose Navigation framework to avoid fragmentation.
</details>

---

## <h2 id="design--ui"><img src="docs/assets/readme_icons/palette.svg" width="24" height="24" align="absmiddle" /> Design & UI</h2>

Design is the true crown jewel of FlickTrove.

- 🪟 **Full Glassmorphism**: We use a custom blur engine to blur the content under panels, drawers, and top bars in real time at 60/120fps.
- 🌈 **Dynamic Theming**: The predominant colors of movie posters and backgrounds are dynamically extracted by Coil to theme the entire screen (gradients, buttons, and accents).
- ✨ **Premium Animations**: Micro-interactions, custom haptic feedback, and bounce-click effects make the app incredibly responsive and "alive" under your fingers.

---

## <h2 id="developer-note"><img src="docs/assets/readme_icons/scudo_privacy.svg" width="24" height="24" align="absmiddle" /> Developer Note</h2>

For security reasons, backend configuration files (such as Firebase's `google-services.json`) and private API keys (TMDB, OMDB, Trakt) have been excluded from this repository.

Therefore, the project cannot be cloned and compiled "out-of-the-box". The source code is publicly exposed **just for reference and sharing**, rather than as a plug-and-play application.

---

## <h2 id="usage"><img src="docs/assets/readme_icons/play.svg" width="24" height="24" align="absmiddle" /> Usage</h2>

Once the app is launched:

1. Log in or create an account (data will be securely saved on Firebase).
2. Use the search bar to find your first movie or TV show.
3. Click "To watch" and select which folder to put it in.
4. *(Optional)* Go to the "Stats" tab to monitor your watch time.

---

## <h2 id="future-roadmap"><img src="docs/assets/readme_icons/calendario.svg" width="24" height="24" align="absmiddle" /> Future Roadmap</h2>

FlickTrove is constantly evolving. In the future, the app will expand beyond a personal tracker into a fully social experience:

- [ ] **Social Integration:** Follow your friends and browse their personal libraries.
- [ ] **Shared Reviews:** Write custom reviews and rate movies, sharing your thoughts with the community.
- [ ] **Global Trends & Leaderboards:** Discover what's trending this week among other FlickTrove users.
- [ ] **Shared Folders:** Create collaborative watchlists with your partner or friends.
- [ ] **AI:** ?

---

## <h2 id="license"><img src="docs/assets/readme_icons/documento.svg" width="24" height="24" align="absmiddle" /> License</h2>

Copyright © 2026 Alessandro Basile. All rights reserved.

This repository is public for reference and sharing. Reproduction, copying, modification, or redistribution of the code, as well as its use for commercial or non-commercial purposes, is strictly prohibited without the explicit written permission of the author. Please consult the `TERMS_OF_SERVICE.md` file for further details.

---

## <h2 id="support-the-project"><img src="docs/assets/readme_icons/heart.svg" width="24" height="24" align="absmiddle" /> Support the Project</h2>

FlickTrove is, and will always be, completely free, open-source, and ad-free. If this app is helping you organize your cinema and TV library, consider supporting its development! Every little bit helps keep the project alive and expanding.

<br>

<div align="center">
  <a href="https://github.com/Alle-0/FlickTrove">
    <img src="https://img.shields.io/github/stars/Alle-0/FlickTrove?style=for-the-badge&color=FFD700&label=Star%20The%20Repo&logo=github&logoColor=black" alt="Star the Repo" />
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://www.paypal.me/AlessandroBasile0" target="_blank">
    <img src="https://img.shields.io/badge/Donate-PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white" alt="Donate via PayPal" />
  </a>
</div>

<br>

---

## <h2 id="contacts--credits"><img src="docs/assets/readme_icons/people.svg" width="24" height="24" align="absmiddle" /> Contacts & Credits</h2>

**Developed by:**

- 👨‍💻 **Alessandro Basile** - <alessandrobasile909@gmail.com>
- 🔗 **Project Link:** [FlickTrove on GitHub](https://github.com/Alle-0/FlickTrove) or [FlickTrove site](https://alle-0.github.io/FlickTrove)

**Credits & Useful Resources:**

- 🎬 [The Movie Database (TMDB) API](https://www.themoviedb.org/documentation/api) - For movie and TV show data
- 🛡️ [Shields.io](https://shields.io) - For README badges

<br>

<div align="center">
  <i>Developed with passion for movie and TV show lovers.</i>
</div>
