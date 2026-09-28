# Remote Compose Android Sample

웹에서 만든 UI를 AndroidX `RemoteComposePlayer`로 재생하는 Android 샘플입니다. 앱은 JSON을 해석하거나 문서를 생성하지 않고, GitHub의 `.rc` 바이너리를 다운로드해 표시합니다.

**[웹 Editor](https://kangmin1012.github.io/RemoteCompose_Sample_Web/)** · [Editor 및 변환기 소스](https://github.com/kangmin1012/RemoteCompose_Sample_Web)

## 실행

1. Android Studio의 **File → Open**에서 `settings.gradle.kts`와 `gradlew`가 들어 있는 이 폴더(`RemoteCompose/RemoteComposeSample`)를 엽니다. 상위 `RemoteCompose` 폴더나 `app` 폴더를 선택하지 않습니다. JDK 21과 프로젝트가 요구하는 Android SDK 36.1을 준비합니다.
2. Android 10(API 29) 이상의 기기 또는 에뮬레이터에서 `app`을 실행합니다.
3. Home 화면의 버튼으로 Detail 화면을 열거나, 상단 새로고침으로 최신 문서를 불러옵니다.
4. 웹 Editor에서 수정하고 Deploy한 뒤 Actions 완료를 기다립니다. 앱을 새로고침하면 변경사항이 반영됩니다.

공개 저장소를 사용하므로 앱에는 GitHub token이 필요하지 않습니다. 다운로드 주소는 `navigation/AppNavigation.kt`의 `SampleScreen.url` 한 곳에서 설정합니다.

### Android Studio에서 Gradle 빌드를 찾지 못하는 경우

`Directory '...' does not contain a Gradle build`가 나오면 오류에 표시된 경로에 `settings.gradle.kts`가 있는지 확인합니다. 프로젝트를 이동한 뒤 Recent Projects에서 이전 위치를 열면 `.idea`만 남은 폴더를 열 수 있습니다.

현재 작업 공간에서는 `/Users/kangmingu/Desktop/RemoteCompose/RemoteComposeSample`을 열어야 합니다. 이전 경로인 `/Users/kangmingu/Desktop/RemoteComposeSample`과 구분하세요. 다른 환경에서는 저장소를 내려받은 실제 위치를 사용합니다.

잘못 연 프로젝트를 닫고 **File → Open**으로 실제 폴더를 다시 연 다음 **Sync Project with Gradle Files**를 실행합니다. Gradle 배포판은 프로젝트의 `gradle/wrapper/gradle-wrapper.properties`를 사용합니다. 이 오류를 해결하려고 `gradle init`으로 새 프로젝트를 만들 필요는 없습니다.

## 코드 읽는 순서

소스 패키지: `app/src/main/java/kang/mingu/remotecomposesample/`

1. `MainActivity.kt`: Compose 테마와 탐색 화면 시작.
2. `navigation/AppNavigation.kt`: Home, Detail의 문서 주소와 화면 이동.
3. `MainViewModel.kt`: 로딩·성공·실패 상태 및 새로고침.
4. `data/remote/RemoteConfigFetcher.kt`: 바이너리 다운로드, HTTP 오류 및 빈 응답 처리.
5. `ui/screen/RemoteScreen.kt`: 로딩·재시도·문서 표시와 액션 분기.
6. `ui/components/RemoteDocumentView.kt`: `AndroidView` 안에서 `RemoteComposePlayer` 사용.

`navigate:<화면 ID>` 액션은 화면을 이동합니다. 나머지 호스트 액션은 ID와 메타데이터를 Toast로 보여줍니다. 기본 화면 이동 예제는 Home → Detail 하나이며, 돌아가기는 앱 상단 뒤로가기 또는 시스템 뒤로가기를 사용합니다.

## 검증

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest # 실행 중인 기기 필요
```

단위 테스트는 바이너리 보존, HTTP 오류, 빈 응답을 확인합니다. 기기 테스트는 웹 변환기로 생성한 두 문서가 실제로 렌더링되는지 검사합니다. 테스트용 `.rc`는 `app/src/androidTest/assets/`에 있습니다.

카드 테두리 회귀 테스트는 둥근 카드와 각진 카드의 배경색 및 1dp·4dp 테두리 색상과 두께를 실제 픽셀에서 검사합니다. `card-border-*.rc`는 웹 저장소의 `server/src/test/fixtures/`에서 생성하며, 변환기의 현재 기록 단위인 2.625px/dp를 기준으로 검증합니다.

앱 1.1은 GitHub Pages의 `config.manifest.json` 또는 `config_detail.manifest.json`에서 배포 문서 경로와 SHA-256을 확인합니다. 문서는 해시가 들어간 고유 파일명으로 받으며 해시가 맞지 않으면 적용하지 않고 오류를 표시합니다. 각 요청에는 고유한 `_refresh` 쿼리를 붙입니다. 앱 시작·수동 새로고침·백그라운드 복귀 때 명세를 다시 확인합니다. 상단의 `최근 확인`은 다운로드가 성공한 시간이고, 화면 문서가 바뀌면 시간과 별개의 문서 revision으로 플레이어를 교체합니다. 동일 문서는 플레이어를 유지합니다. 상단의 `화면` 뒤 8자리는 적용 문서 해시이므로 배포 명세와 비교할 수 있습니다.

새로고침 회귀 테스트는 URL별 CDN 캐시, 동일 문서 재확인, 같은 시각에 변경된 문서, 로딩/실패 상태를 검증합니다. 기기 테스트의 `refresh-before.rc`와 `refresh-after.rc`는 웹 저장소의 `bb4c5b5`와 `6aadcf0` 문서이며, 실제 새로고침 버튼을 눌러 세 번째 버튼이 추가된 문서로 화면이 달라지는지 확인합니다.

## 참고 코드 및 범위

[armcha/remotecompose-android](https://github.com/armcha/remotecompose-android), 기준 커밋 `86382fa33837a14031a76610471c85205d127ff1`의 플레이어 구조를 참고했습니다. 샘플에서는 앱 내 토큰 설정을 없애고 상태 관리와 화면 등록을 간결하게 구성했습니다.

앱과 웹 변환기는 `androidx.compose.remote:1.0.0-alpha20`로 버전을 맞춥니다. 이 버전의 View 플레이어는 실험적 제한 API이므로 `RemoteDocumentView`에만 Lint 사용 표시를 적용했습니다. 다른 버전으로 변경할 때는 변환기와 플레이어의 호환성을 함께 검증하세요.

2026-09-28 공식 릴리스 및 Google Maven 재확인 결과 최신 RemoteCompose는 `1.0.0-alpha20`이며 앱과 변환기 모두 이미 이 버전을 사용합니다.

공개 배포까지 확인하려면 명세의 전체 `sha256`을 `expectedDocumentSha256` instrumentation 인자로 주고 `PublishedScreenTest`를 실행합니다. 실제 네트워크 다운로드, MainActivity 최초 실행·새로고침·복귀·재생성을 확인합니다. 일반 테스트 실행에서는 이 네트워크 테스트를 건너뜁니다.
