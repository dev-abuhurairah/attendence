# Student Attendance App

Android app (Kotlin + Jetpack Compose) for taking class attendance.

## How to get the APK from GitHub

1. Create a new repository on GitHub.
2. Upload all files from this folder (keep the `.github` folder).
3. Push to the `main` branch. The build starts by itself.
4. Open the **Actions** tab, click the latest run, and download **attendance-app-apk** at the bottom.
5. Unzip it and install `app-debug.apk` on your phone.

You can also start a build by hand: Actions > Build APK > Run workflow.

## How the app works

- **Setup > Classes**: add a class (for example BBA) and its number of semesters.
- **Setup > Teachers**: add teacher names.
- **Setup > Subjects**: choose class and semester, then add subjects with a teacher.
- **Setup > Students**: choose class and semester, then add students. Write one per line as `Roll No, Name`.
- **Home**: every class gets a button. Tap it, pick a semester, then **Attendance** or **Report & Excel**.
- **Attendance**: pick a subject and date, mark P (present), A (absent) or L (leave), then Save.
- **Report & Excel**: see totals and percentages, and create an `.xlsx` file in the Downloads folder.
  The file has a Summary sheet and one sheet for each subject.

Works on Android 10 (API 29) and newer. All data is stored on the phone.
