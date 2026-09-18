# Remote Compose Android Sample

웹에서 만든 UI를 AndroidX `RemoteComposePlayer`로 재생하는 Android 샘플입니다. 앱은 JSON을 해석하거나 문서를 생성하지 않고, GitHub의 `.rc` 바이너리를 다운로드해 표시합니다.

**[웹 Editor](https://kangmin1012.github.io/RemoteCompose_Sample_Web/)** · [Editor 및 변환기 소스](https://github.com/kangmin1012/RemoteCompose_Sample_Web)

## 실행

1. Android Studio에서 이 폴더를 엽니다. JDK 21과 프로젝트가 요구하는 Android SDK 36.1을 준비합니다.
2. Android 10(API 29) 이상의 기기 또는 에뮬레이터에서 `app`을 실행합니다.
3. Home 화면의 버튼으로 Detail 화면을 열거나, 상단 새로고침으로 최신 문서를 불러옵니다.
4. 웹 Editor에서 수정하고 Deploy한 뒤 Actions 완료를 기다립니다. 앱을 새로고침하면 변경사항이 반영됩니다.

공개 저장소를 사용하므로 앱에는 GitHub token이 필요하지 않습니다. 다운로드 주소는 `navigation/AppNavigation.kt`의 `SampleScreen.url` 한 곳에서 설정합니다.

## 코드 읽는 순서

소스 패키지: `app/src/main/java/kang/mingu/remotecomposesample/`

1. `MainActivity.kt`: Compose 테마와 탐색 화면 시작.
2. `navigation/AppNavigation.kt`: Home, Detail, Estimates, Estimate Detail의 문서 주소와 화면 이동.
3. `MainViewModel.kt`: 로딩·성공·실패 상태 및 새로고침.
4. `data/remote/RemoteConfigFetcher.kt`: 바이너리 다운로드, HTTP 오류 및 빈 응답 처리.
5. `ui/screen/RemoteScreen.kt`: 로딩·재시도·문서 표시와 액션 분기.
6. `ui/components/RemoteDocumentView.kt`: `AndroidView` 안에서 `RemoteComposePlayer` 사용.

`navigate:<화면 ID>` 액션은 화면을 이동합니다. 나머지 호스트 액션은 ID와 메타데이터를 Toast로 보여줍니다. 화면 이름을 목록으로 관리해 동일한 화면 코드를 중복 작성하지 않습니다.

## 검증

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest # 실행 중인 기기 필요
```

단위 테스트는 바이너리 보존, HTTP 오류, 빈 응답을 확인합니다. 기기 테스트는 웹 변환기로 생성한 네 문서가 실제로 렌더링되는지 검사합니다. 테스트용 `.rc`는 `app/src/androidTest/assets/`에 있습니다.

## 참고 코드 및 범위

[armcha/remotecompose-android](https://github.com/armcha/remotecompose-android), 기준 커밋 `86382fa33837a14031a76610471c85205d127ff1`의 플레이어 구조를 참고했습니다. 샘플에서는 앱 내 토큰 설정을 없애고 상태 관리와 화면 등록을 간결하게 구성했습니다.

앱과 웹 변환기는 `androidx.compose.remote:1.0.0-alpha05`로 버전을 맞춥니다. 이 버전의 View 플레이어는 실험적 제한 API이므로 `RemoteDocumentView`에만 Lint 사용 표시를 적용했습니다. 다른 버전으로 변경할 때는 변환기와 플레이어의 호환성을 함께 검증하세요.
