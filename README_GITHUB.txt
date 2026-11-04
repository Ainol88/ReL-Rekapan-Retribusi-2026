PANDUAN GITHUB - REKAPAN RETRIBUSI JASA KEPELABUHANAN 2026

1. Buat repository baru di https://github.com/new
2. Beri nama: rekapan-retribusi-2026
3. Pilih Public atau Private, lalu klik Create repository.
4. Ekstrak ZIP paket GitHub ini di komputer.
5. Buka repository GitHub, pilih Add file > Upload files.
6. Upload SEMUA ISI folder hasil ekstrak (termasuk folder .github).
   Pastikan file build.gradle, settings.gradle, gradle.properties,
   folder app, dan folder .github berada langsung di root repository.
7. Klik Commit changes.
8. Buka tab Actions. Workflow 'Build APK Android' akan berjalan otomatis.
9. Setelah selesai dengan tanda hijau, buka hasil workflow tersebut.
10. Di bagian Artifacts, unduh 'Rekapan-Retribusi-2026-APK'.
11. Ekstrak ZIP artifact. Di dalamnya ada app-debug.apk.
12. Kirim APK ke HP Android dan instal. Android mungkin meminta izin
    memasang aplikasi dari sumber tersebut.

Jika workflow tidak berjalan otomatis: buka Actions > Build APK Android >
Run workflow > Run workflow.

CATATAN: GitHub menyimpan source code dan menjalankan proses build; GitHub
bukan tempat menjalankan aplikasi Android. APK yang dihasilkan adalah APK
Debug untuk uji coba/internal, belum APK rilis bertanda tangan untuk distribusi publik.
