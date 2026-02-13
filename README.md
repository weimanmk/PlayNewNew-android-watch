# 项目说明

基于原牛牛挤奶项目的安卓手表迁移，基于API30的方表，功能与原版几乎一样，增加灵敏度自动校正，gpt5.3-codex辅助开发

## 安装包
链接：https://pan.quark.cn/s/ba0014657179
https://pan.baidu.com/s/1c51fBp8AQErRL_PX-oCyjA?pwd=dnph

## 构建

```bash
./gradlew :app:assembleRelease
```

Windows:

```bat
.\gradlew.bat :app:assembleRelease
```

## 安装到设备

```bat
adb install -r app\build\outputs\apk\release\app-release.apk
```

## 权限说明

- `android.permission.BODY_SENSORS`：用于读取心率
- `android.permission.VIBRATE`：用于震动反馈
- `android.permission.WAKE_LOCK`：用于保持运行稳定

## 开源协议

本项目与原项目一样，遵循 MIT License。
