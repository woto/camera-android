package com.example.cameracontrol

import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    ZH("zh", "中文"),
    ES("es", "Español"),
    AR("ar", "العربية"),
    HI("hi", "Hindi"),
    BN("bn", "Bengali"),
    KO("ko", "한국어"),
    ID("id", "Bahasa Indonesia"),
    PT("pt", "Português"),
    FR("fr", "Français"),
    IT("it", "Italiano"),
    JA("ja", "日本語"),
    TR("tr", "Türkçe"),
    VI("vi", "Tiếng Việt"),
    TH("th", "ไทย"),
    RU("ru", "Русский"),
    DE("de", "Deutsch"),
    PL("pl", "Polski"),
    NL("nl", "Nederlands"),
    UR("ur", "اردو"),
    FA("fa", "فارسی"),
    SR("sr", "Srpski");

    companion object {
        fun fromCode(code: String?): AppLanguage? {
            if (code.isNullOrBlank()) return null
            return values().firstOrNull { it.code == code.lowercase() }
        }
    }
}

object AppStrings {
    fun get(id: String, language: AppLanguage): String {
        return translations[language]?.get(id)
            ?: translations[AppLanguage.EN]?.get(id)
            ?: id
    }

    fun defaultLanguage(locale: Locale): AppLanguage {
        return when (locale.language.lowercase()) {
            "zh" -> AppLanguage.ZH
            "es" -> AppLanguage.ES
            "ar" -> AppLanguage.AR
            "hi" -> AppLanguage.HI
            "bn" -> AppLanguage.BN
            "ko" -> AppLanguage.KO
            "id", "in" -> AppLanguage.ID
            "pt" -> AppLanguage.PT
            "fr" -> AppLanguage.FR
            "it" -> AppLanguage.IT
            "ja" -> AppLanguage.JA
            "tr" -> AppLanguage.TR
            "vi" -> AppLanguage.VI
            "th" -> AppLanguage.TH
            "ru" -> AppLanguage.RU
            "de" -> AppLanguage.DE
            "pl" -> AppLanguage.PL
            "nl" -> AppLanguage.NL
            "ur" -> AppLanguage.UR
            "fa" -> AppLanguage.FA
            "sr" -> AppLanguage.SR
            else -> AppLanguage.EN
        }
    }

    private val translations: Map<AppLanguage, Map<String, String>> = mapOf(
        AppLanguage.EN to mapOf(
            "welcome" to "Welcome",
            "intro_text" to "Record volleyball highlights with your phone. " +
                "To save battery, recording can continue with the screen off. " +
                "Stop recording when you are done.",
            "ok" to "OK",
            "room_id_title" to "Room ID",
            "room_id_prompt" to "Enter the Room ID for this session:",
            "permissions_required" to "Permissions required!",
            "camera_permissions_msg" to "Please grant camera and audio permissions to continue.",
            "starting_camera" to "Starting camera service...",
            "debug_logs" to "Debug Logs:",
            "simulate_save" to "Save recording",
            "zoom" to "Zoom",
            "exit" to "Exit",
            "privacy_disclaimer" to "By tapping OK, you agree to our",
            "privacy_link" to "Privacy Policy",
            "language_label" to "Language",
            "website_label" to "Website",
            "alert_phone_flat" to "Phone is lying flat.",
            "alert_ws_disconnected" to "No connection to server."
        ),
        AppLanguage.ZH to mapOf(
            "welcome" to "欢迎",
            "intro_text" to "用手机记录精彩的排球时刻。为节省电量，" +
                "即使屏幕关闭也可继续录制。使用完请停止录制。",
            "ok" to "确定",
            "room_id_title" to "房间 ID",
            "room_id_prompt" to "请输入本次会话的房间 ID：",
            "permissions_required" to "需要权限！",
            "camera_permissions_msg" to "请授予相机和音频权限以继续。",
            "starting_camera" to "正在启动相机服务...",
            "debug_logs" to "调试日志：",
            "simulate_save" to "保存录制",
            "zoom" to "变焦",
            "exit" to "退出",
            "privacy_disclaimer" to "点击确定即表示你同意我们的",
            "privacy_link" to "隐私政策",
            "language_label" to "语言",
            "website_label" to "网站",
            "alert_phone_flat" to "手机正平放。",
            "alert_ws_disconnected" to "未连接到服务器。"
        ),
        AppLanguage.ES to mapOf(
            "welcome" to "Bienvenido",
            "intro_text" to "Graba momentos destacados de voleibol con tu teléfono. " +
                "Para ahorrar batería, la grabación puede continuar con la pantalla apagada. " +
                "Detén la grabación cuando termines.",
            "ok" to "OK",
            "room_id_title" to "ID de sala",
            "room_id_prompt" to "Introduce el ID de la sala para esta sesión:",
            "permissions_required" to "¡Se requieren permisos!",
            "camera_permissions_msg" to "Concede permisos de cámara y audio para continuar.",
            "starting_camera" to "Iniciando servicio de cámara...",
            "debug_logs" to "Registros de depuración:",
            "simulate_save" to "Guardar grabación",
            "zoom" to "Zoom",
            "exit" to "Salir",
            "privacy_disclaimer" to "Al pulsar OK, aceptas nuestra",
            "privacy_link" to "Política de privacidad",
            "language_label" to "Idioma",
            "website_label" to "Sitio web",
            "alert_phone_flat" to "El teléfono está en posición plana.",
            "alert_ws_disconnected" to "No hay conexión con el servidor."
        ),
        AppLanguage.AR to mapOf(
            "welcome" to "مرحبًا",
            "intro_text" to "سجّل لحظات جميلة لكرة الطائرة بهاتفك. " +
                "لتوفير البطارية، يمكن أن يستمر التسجيل والشاشة مطفأة. " +
                "أوقف التسجيل عند الانتهاء.",
            "ok" to "حسنًا",
            "room_id_title" to "معرّف الغرفة",
            "room_id_prompt" to "أدخل معرّف الغرفة لهذه الجلسة:",
            "permissions_required" to "الأذونات مطلوبة!",
            "camera_permissions_msg" to "يرجى منح أذونات الكاميرا والصوت للمتابعة.",
            "starting_camera" to "بدء خدمة الكاميرا...",
            "debug_logs" to "سجلات التصحيح:",
            "simulate_save" to "حفظ التسجيل",
            "zoom" to "تكبير",
            "exit" to "خروج",
            "privacy_disclaimer" to "بالنقر على موافق، فإنك توافق على",
            "privacy_link" to "سياسة الخصوصية",
            "language_label" to "اللغة",
            "website_label" to "الموقع",
            "alert_phone_flat" to "الهاتف موضوع بشكل مسطح.",
            "alert_ws_disconnected" to "لا يوجد اتصال بالخادم."
        ),
        AppLanguage.HI to mapOf(
            "welcome" to "स्वागत है",
            "intro_text" to "अपने फोन से वॉलीबॉल के बेहतरीन पल रिकॉर्ड करें। " +
                "बैटरी बचाने के लिए स्क्रीन बंद होने पर भी रिकॉर्डिंग चल सकती है। " +
                "काम खत्म होने पर रिकॉर्डिंग रोक दें।",
            "ok" to "ठीक है",
            "room_id_title" to "रूम आईडी",
            "room_id_prompt" to "इस सत्र के लिए रूम आईडी दर्ज करें:",
            "permissions_required" to "अनुमतियाँ आवश्यक हैं!",
            "camera_permissions_msg" to "जारी रखने के लिए कैमरा और ऑडियो अनुमतियाँ दें।",
            "starting_camera" to "कैमरा सेवा शुरू हो रही है...",
            "debug_logs" to "डीबग लॉग:",
            "simulate_save" to "रिकॉर्डिंग सहेजें",
            "zoom" to "ज़ूम",
            "exit" to "बाहर निकलें",
            "privacy_disclaimer" to "OK पर टैप करके आप हमारी सहमति देते हैं",
            "privacy_link" to "गोपनीयता नीति",
            "language_label" to "भाषा",
            "website_label" to "वेबसाइट",
            "alert_phone_flat" to "फ़ोन समतल रखा है।",
            "alert_ws_disconnected" to "सर्वर से कनेक्शन नहीं है।"
        ),
        AppLanguage.BN to mapOf(
            "welcome" to "স্বাগতম",
            "intro_text" to "আপনার ফোন দিয়ে ভলিবলের সুন্দর মুহূর্তগুলো রেকর্ড করুন। " +
                "ব্যাটারি বাঁচাতে স্ক্রিন বন্ধ থাকলেও রেকর্ডিং চলতে পারে। " +
                "কাজ শেষ হলে রেকর্ডিং বন্ধ করুন।",
            "ok" to "ঠিক আছে",
            "room_id_title" to "রুম আইডি",
            "room_id_prompt" to "এই সেশনের জন্য রুম আইডি দিন:",
            "permissions_required" to "অনুমতি প্রয়োজন!",
            "camera_permissions_msg" to "চালিয়ে যেতে ক্যামেরা ও অডিও অনুমতি দিন।",
            "starting_camera" to "ক্যামেরা সার্ভিস চালু হচ্ছে...",
            "debug_logs" to "ডিবাগ লগ:",
            "simulate_save" to "রেকর্ডিং সংরক্ষণ করুন",
            "zoom" to "জুম",
            "exit" to "বেরিয়ে যান",
            "privacy_disclaimer" to "OK ট্যাপ করলে আপনি আমাদের গ্রহণ করছেন",
            "privacy_link" to "গোপনীয়তা নীতি",
            "language_label" to "ভাষা",
            "website_label" to "ওয়েবসাইট",
            "alert_phone_flat" to "ফোনটি সমতল অবস্থায় আছে।",
            "alert_ws_disconnected" to "সার্ভারের সাথে সংযোগ নেই।"
        ),
        AppLanguage.KO to mapOf(
            "welcome" to "환영합니다",
            "intro_text" to "휴대폰으로 배구의 멋진 순간을 녹화하세요. " +
                "배터리 절약을 위해 화면이 꺼진 상태에서도 녹화가 계속될 수 있습니다. " +
                "사용 후 녹화를 중지하세요.",
            "ok" to "확인",
            "room_id_title" to "룸 ID",
            "room_id_prompt" to "이 세션의 룸 ID를 입력하세요:",
            "permissions_required" to "권한이 필요합니다!",
            "camera_permissions_msg" to "계속하려면 카메라 및 오디오 권한을 허용하세요.",
            "starting_camera" to "카메라 서비스를 시작하는 중...",
            "debug_logs" to "디버그 로그:",
            "simulate_save" to "녹화 저장",
            "zoom" to "줌",
            "exit" to "종료",
            "privacy_disclaimer" to "OK를 누르면 다음에 동의합니다:",
            "privacy_link" to "개인정보 처리방침",
            "language_label" to "언어",
            "website_label" to "웹사이트",
            "alert_phone_flat" to "휴대폰이 평평하게 놓여 있습니다.",
            "alert_ws_disconnected" to "서버와 연결되어 있지 않습니다."
        ),
        AppLanguage.ID to mapOf(
            "welcome" to "Selamat datang",
            "intro_text" to "Rekam momen voli yang indah dengan ponsel Anda. " +
                "Untuk menghemat baterai, perekaman dapat berlanjut saat layar mati. " +
                "Hentikan perekaman setelah selesai.",
            "ok" to "OK",
            "room_id_title" to "ID Ruangan",
            "room_id_prompt" to "Masukkan ID ruangan untuk sesi ini:",
            "permissions_required" to "Izin diperlukan!",
            "camera_permissions_msg" to "Berikan izin kamera dan audio untuk melanjutkan.",
            "starting_camera" to "Memulai layanan kamera...",
            "debug_logs" to "Log debug:",
            "simulate_save" to "Simpan rekaman",
            "zoom" to "Zoom",
            "exit" to "Keluar",
            "privacy_disclaimer" to "Dengan menekan OK, Anda menyetujui",
            "privacy_link" to "Kebijakan Privasi",
            "language_label" to "Bahasa",
            "website_label" to "Situs web",
            "alert_phone_flat" to "Ponsel sedang terletak datar.",
            "alert_ws_disconnected" to "Tidak ada koneksi ke сервер."
        ),
        AppLanguage.PT to mapOf(
            "welcome" to "Bem-vindo",
            "intro_text" to "Grave momentos incríveis de vôlei com seu telefone. " +
                "Para economizar bateria, a gravação pode continuar com a tela apagada. " +
                "Pare a gravação ao terminar.",
            "ok" to "OK",
            "room_id_title" to "ID da sala",
            "room_id_prompt" to "Digite o ID da sala para esta sessão:",
            "permissions_required" to "Permissões necessárias!",
            "camera_permissions_msg" to "Conceda permissões de câmera e áudio para continuar.",
            "starting_camera" to "Iniciando serviço da câmera...",
            "debug_logs" to "Logs de depuração:",
            "simulate_save" to "Salvar gravação",
            "zoom" to "Zoom",
            "exit" to "Sair",
            "privacy_disclaimer" to "Ao tocar em OK, você concorda com nossa",
            "privacy_link" to "Política de Privacidade",
            "language_label" to "Idioma",
            "website_label" to "Site",
            "alert_phone_flat" to "O telefone está deitado.",
            "alert_ws_disconnected" to "Sem conexão com o servidor."
        ),
        AppLanguage.FR to mapOf(
            "welcome" to "Bienvenue",
            "intro_text" to "Enregistrez de beaux moments de volley avec votre téléphone. " +
                "Pour économiser la batterie, l'enregistrement peut continuer écran éteint. " +
                "Arrêtez l'enregistrement une fois terminé.",
            "ok" to "OK",
            "room_id_title" to "ID de la salle",
            "room_id_prompt" to "Saisissez l'ID de la salle pour cette session :",
            "permissions_required" to "Autorisations requises !",
            "camera_permissions_msg" to "Accordez les autorisations caméra et audio pour continuer.",
            "starting_camera" to "Démarrage du service caméra...",
            "debug_logs" to "Journaux de débogage :",
            "simulate_save" to "Enregistrer la vidéo",
            "zoom" to "Zoom",
            "exit" to "Quitter",
            "privacy_disclaimer" to "En appuyant sur OK, vous acceptez notre",
            "privacy_link" to "Politique de confidentialité",
            "language_label" to "Langue",
            "website_label" to "Site web",
            "alert_phone_flat" to "Le téléphone est posé à plat.",
            "alert_ws_disconnected" to "Pas de connexion au serveur."
        ),
        AppLanguage.IT to mapOf(
            "welcome" to "Benvenuto",
            "intro_text" to "Registra i momenti più belli della pallavolo con il tuo telefono. " +
                "Per risparmiare batteria, la registrazione può continuare con lo schermo spento. " +
                "Interrompi la registrazione quando hai finito.",
            "ok" to "OK",
            "room_id_title" to "ID stanza",
            "room_id_prompt" to "Inserisci l'ID stanza per questa sessione:",
            "permissions_required" to "Permessi richiesti!",
            "camera_permissions_msg" to "Concedi i permessi per fotocamera e audio per continuare.",
            "starting_camera" to "Avvio del servizio fotocamera...",
            "debug_logs" to "Log di debug:",
            "simulate_save" to "Salva registrazione",
            "zoom" to "Zoom",
            "exit" to "Esci",
            "privacy_disclaimer" to "Toccando OK, accetti la nostra",
            "privacy_link" to "Informativa sulla privacy",
            "language_label" to "Lingua",
            "website_label" to "Sito web",
            "alert_phone_flat" to "Il telefono è appoggiato in piano.",
            "alert_ws_disconnected" to "Nessuna connessione al server."
        ),
        AppLanguage.JA to mapOf(
            "welcome" to "ようこそ",
            "intro_text" to "スマートフォンでバレーボールの名場面を録画します。" +
                "電池節約のため、画面が消えても録画を続けられます。" +
                "使用後は録画を停止してください。",
            "ok" to "OK",
            "room_id_title" to "ルーム ID",
            "room_id_prompt" to "このセッションのルーム ID を入力してください：",
            "permissions_required" to "権限が必要です！",
            "camera_permissions_msg" to "続行するにはカメラと音声の権限を付与してください。",
            "starting_camera" to "カメラサービスを開始しています...",
            "debug_logs" to "デバッグログ：",
            "simulate_save" to "録画を保存",
            "zoom" to "ズーム",
            "exit" to "終了",
            "privacy_disclaimer" to "OK をタップすると、次に同意したことになります：",
            "privacy_link" to "プライバシーポリシー",
            "language_label" to "言語",
            "website_label" to "ウェブサイト",
            "alert_phone_flat" to "スマホが平置きです。",
            "alert_ws_disconnected" to "サーバーへの接続がありません。"
        ),
        AppLanguage.TR to mapOf(
            "welcome" to "Hoş geldiniz",
            "intro_text" to "Telefonunuzla voleybolun güzel anlarını kaydedin. " +
                "Pil tasarrufu için ekran kapalıyken de kayıt devam edebilir. " +
                "İşiniz bitince kaydı durdurun.",
            "ok" to "Tamam",
            "room_id_title" to "Oda ID",
            "room_id_prompt" to "Bu oturum için oda ID'sini girin:",
            "permissions_required" to "İzinler gerekli!",
            "camera_permissions_msg" to "Devam etmek için kamera ve ses izinleri verin.",
            "starting_camera" to "Kamera hizmeti başlatılıyor...",
            "debug_logs" to "Hata ayıklama günlükleri:",
            "simulate_save" to "Kaydı kaydet",
            "zoom" to "Yakınlaştırma",
            "exit" to "Çıkış",
            "privacy_disclaimer" to "OK'a dokunarak şunları kabul edersiniz:",
            "privacy_link" to "Gizlilik Politikası",
            "language_label" to "Dil",
            "website_label" to "Web sitesi",
            "alert_phone_flat" to "Telefon düz duruyor.",
            "alert_ws_disconnected" to "Sunucu bağlantısı yok."
        ),
        AppLanguage.VI to mapOf(
            "welcome" to "Chào mừng",
            "intro_text" to "Ghi lại những khoảnh khắc bóng chuyền đẹp bằng điện thoại của bạn. " +
                "Để tiết kiệm pin, việc ghi có thể tiếp tục khi màn hình tắt. " +
                "Hãy dừng ghi khi bạn dùng xong.",
            "ok" to "OK",
            "room_id_title" to "ID phòng",
            "room_id_prompt" to "Nhập ID phòng cho phiên này:",
            "permissions_required" to "Cần cấp quyền!",
            "camera_permissions_msg" to "Vui lòng cấp quyền camera và âm thanh để tiếp tục.",
            "starting_camera" to "Đang khởi động dịch vụ camera...",
            "debug_logs" to "Nhật ký gỡ lỗi:",
            "simulate_save" to "Lưu bản ghi",
            "zoom" to "Thu phóng",
            "exit" to "Thoát",
            "privacy_disclaimer" to "Bằng cách nhấn OK, bạn đồng ý với",
            "privacy_link" to "Chính sách quyền riêng tư",
            "language_label" to "Ngôn ngữ",
            "website_label" to "Trang web",
            "alert_phone_flat" to "Điện thoại đang nằm phẳng.",
            "alert_ws_disconnected" to "Không có kết nối tới máy chủ."
        ),
        AppLanguage.TH to mapOf(
            "welcome" to "ยินดีต้อนรับ",
            "intro_text" to "บันทึกช่วงเวลาวอลเลย์บอลที่สวยงามด้วยโทรศัพท์ของคุณ " +
                "เพื่อประหยัดแบตเตอรี่ การบันทึกสามารถทำต่อได้เมื่อหน้าจอดับ " +
                "หยุดบันทึกเมื่อใช้งานเสร็จแล้ว",
            "ok" to "ตกลง",
            "room_id_title" to "รหัสห้อง",
            "room_id_prompt" to "กรอกรหัสห้องสำหรับเซสชันนี้:",
            "permissions_required" to "ต้องขออนุญาต!",
            "camera_permissions_msg" to "โปรดอนุญาตการเข้าถึงกล้องและเสียงเพื่อดำเนินการต่อ",
            "starting_camera" to "กำลังเริ่มบริการกล้อง...",
            "debug_logs" to "บันทึกดีบัก:",
            "simulate_save" to "บันทึกวิดีโอ",
            "zoom" to "ซูม",
            "exit" to "ออก",
            "privacy_disclaimer" to "เมื่อแตะ OK แสดงว่าคุณยอมรับ",
            "privacy_link" to "นโยบายความเป็นส่วนตัว",
            "language_label" to "ภาษา",
            "website_label" to "เว็บไซต์",
            "alert_phone_flat" to "โทรศัพท์วางราบอยู่.",
            "alert_ws_disconnected" to "ไม่มีการเชื่อมต่อกับเซิร์ฟเวอร์."
        ),
        AppLanguage.RU to mapOf(
            "welcome" to "Добро пожаловать",
            "intro_text" to "Записывайте лучшие моменты волейбола на телефон. " +
                "Для экономии батареи запись может идти при выключенном экране. " +
                "Остановите запись после завершения.",
            "ok" to "ОК",
            "room_id_title" to "ID комнаты",
            "room_id_prompt" to "Введите ID комнаты для этой сессии:",
            "permissions_required" to "Требуются разрешения!",
            "camera_permissions_msg" to "Пожалуйста, предоставьте разрешения на камеру и аудио.",
            "starting_camera" to "Запуск сервиса камеры...",
            "debug_logs" to "Логи отладки:",
            "simulate_save" to "Сохранить запись",
            "zoom" to "Зум",
            "exit" to "Выход",
            "privacy_disclaimer" to "Нажимая ОК, вы принимаете нашу",
            "privacy_link" to "Политику конфиденциальности",
            "language_label" to "Язык",
            "website_label" to "Сайт",
            "alert_phone_flat" to "Телефон лежит плашмя.",
            "alert_ws_disconnected" to "Соединение WebSocket потеряно."
        ),
        AppLanguage.DE to mapOf(
            "welcome" to "Willkommen",
            "intro_text" to "Nimm schöne Volleyball-Momente mit deinem Handy auf. " +
                "Um Akku zu sparen, kann die Aufnahme bei ausgeschaltetem Bildschirm weiterlaufen. " +
                "Beende die Aufnahme, wenn du fertig bist.",
            "ok" to "OK",
            "room_id_title" to "Raum-ID",
            "room_id_prompt" to "Gib die Raum-ID für diese Sitzung ein:",
            "permissions_required" to "Berechtigungen erforderlich!",
            "camera_permissions_msg" to "Bitte Kamera- und Audio-Berechtigungen erteilen.",
            "starting_camera" to "Kamera-Service wird gestartet...",
            "debug_logs" to "Debug-Logs:",
            "simulate_save" to "Aufnahme speichern",
            "zoom" to "Zoom",
            "exit" to "Beenden",
            "privacy_disclaimer" to "Durch Tippen auf OK stimmst du unserer",
            "privacy_link" to "Datenschutzerklärung",
            "language_label" to "Sprache",
            "website_label" to "Webseite",
            "alert_phone_flat" to "Das Telefon liegt flach.",
            "alert_ws_disconnected" to "Keine Verbindung zum Server."
        ),
        AppLanguage.PL to mapOf(
            "welcome" to "Witamy",
            "intro_text" to "Nagrywaj najlepsze momenty siatkówki swoim telefonem. " +
                "Aby oszczędzać baterię, nagrywanie może trwać przy wyłączonym ekranie. " +
                "Zatrzymaj nagrywanie po zakończeniu.",
            "ok" to "OK",
            "room_id_title" to "ID pokoju",
            "room_id_prompt" to "Wpisz ID pokoju dla tej sesji:",
            "permissions_required" to "Wymagane uprawnienia!",
            "camera_permissions_msg" to "Aby kontynuować, przyznaj uprawnienia kamery i dźwięku.",
            "starting_camera" to "Uruchamianie usługi kamery...",
            "debug_logs" to "Logi debugowania:",
            "simulate_save" to "Zapisz nagranie",
            "zoom" to "Zoom",
            "exit" to "Wyjście",
            "privacy_disclaimer" to "Dotykając OK, akceptujesz naszą",
            "privacy_link" to "Politykę prywatności",
            "language_label" to "Język",
            "website_label" to "Strona",
            "alert_phone_flat" to "Telefon leży na płasko.",
            "alert_ws_disconnected" to "Brak połączenia z serwerem."
        ),
        AppLanguage.NL to mapOf(
            "welcome" to "Welkom",
            "intro_text" to "Neem mooie volleybalmomenten op met je telefoon. " +
                "Om batterij te besparen, kan de opname doorgaan met het scherm uit. " +
                "Stop de opname wanneer je klaar bent.",
            "ok" to "OK",
            "room_id_title" to "Kamer-ID",
            "room_id_prompt" to "Voer de kamer-ID voor deze sessie in:",
            "permissions_required" to "Machtigingen vereist!",
            "camera_permissions_msg" to "Geef camera- en audiomachtigingen om door te gaan.",
            "starting_camera" to "Cameraservice starten...",
            "debug_logs" to "Debuglogs:",
            "simulate_save" to "Opname opslaan",
            "zoom" to "Zoom",
            "exit" to "Afsluiten",
            "privacy_disclaimer" to "Door op OK te tikken ga je akkoord met onze",
            "privacy_link" to "Privacyverklaring",
            "language_label" to "Taal",
            "website_label" to "Website",
            "alert_phone_flat" to "De telefoon ligt plat.",
            "alert_ws_disconnected" to "Geen verbinding met de server."
        ),
        AppLanguage.UR to mapOf(
            "welcome" to "خوش آمدید",
            "intro_text" to "اپنے فون سے والی بال کے خوبصورت لمحات ریکارڈ کریں۔ " +
                "بیٹری بچانے کے لیے اسکرین بند ہونے پر بھی ریکارڈنگ جاری رہ سکتی ہے۔ " +
                "کام ختم ہونے پر ریکارڈنگ بند کریں۔",
            "ok" to "ٹھیک ہے",
            "room_id_title" to "روم آئی ڈی",
            "room_id_prompt" to "اس سیشن کے لیے روم آئی ڈی درج کریں:",
            "permissions_required" to "اجازتیں درکار ہیں!",
            "camera_permissions_msg" to "جاری رکھنے کے لیے کیمرا اور آڈیو اجازتیں دیں۔",
            "starting_camera" to "کیمرہ سروس شروع ہو رہی ہے...",
            "debug_logs" to "ڈی بگ لاگز:",
            "simulate_save" to "ریکارڈنگ محفوظ کریں",
            "zoom" to "زوم",
            "exit" to "خروج",
            "privacy_disclaimer" to "OK پر ٹیپ کرنے سے آپ ہماری منظوری دیتے ہیں",
            "privacy_link" to "رازداری کی پالیسی",
            "language_label" to "زبان",
            "website_label" to "ویب سائٹ",
            "alert_phone_flat" to "فون سیدھا رکھا ہوا ہے۔",
            "alert_ws_disconnected" to "سرور سے کنکشن نہیں ہے۔"
        ),
        AppLanguage.FA to mapOf(
            "welcome" to "خوش آمدید",
            "intro_text" to "لحظات زیبای والیبال را با گوشی خود ضبط کنید. " +
                "برای صرفه‌جویی در باتری، ضبط می‌تواند با صفحه خاموش ادامه پیدا کند. " +
                "پس از پایان کار، ضبط را متوقف کنید.",
            "ok" to "باشه",
            "room_id_title" to "شناسه اتاق",
            "room_id_prompt" to "شناسه اتاق این جلسه را وارد کنید:",
            "permissions_required" to "مجوزها لازم است!",
            "camera_permissions_msg" to "برای ادامه، مجوز دوربین و صدا را بدهید.",
            "starting_camera" to "در حال شروع سرویس دوربین...",
            "debug_logs" to "گزارش‌های اشکال‌زدایی:",
            "simulate_save" to "ذخیره ضبط",
            "zoom" to "بزرگ‌نمایی",
            "exit" to "خروج",
            "privacy_disclaimer" to "با زدن OK، شما می‌پذیرید",
            "privacy_link" to "سیاست حریم خصوصی",
            "language_label" to "زبان",
            "website_label" to "وب‌سایت",
            "alert_phone_flat" to "گوشی به صورت صاف قرار دارد.",
            "alert_ws_disconnected" to "اتصال به سرور وجود ندارد."
        ),
        AppLanguage.SR to mapOf(
            "welcome" to "Dobro došli",
            "intro_text" to "Snimajte lepe odbojkaške trenutke telefonom. " +
                "Radi uštede baterije, snimanje može da traje i kada je ekran ugašen. " +
                "Zaustavite snimanje kada završite.",
            "ok" to "U redu",
            "room_id_title" to "ID sobe",
            "room_id_prompt" to "Unesite ID sobe za ovu sesiju:",
            "permissions_required" to "Potrebne su dozvole!",
            "camera_permissions_msg" to "Dodelite dozvole za kameru i audio da biste nastavili.",
            "starting_camera" to "Pokretanje usluge kamere...",
            "debug_logs" to "Debug logovi:",
            "simulate_save" to "Sačuvaj snimak",
            "zoom" to "Zum",
            "exit" to "Izlaz",
            "privacy_disclaimer" to "Dodirom na OK prihvatate našu",
            "privacy_link" to "Politiku privatnosti",
            "language_label" to "Jezik",
            "website_label" to "Veb-sajt",
            "alert_phone_flat" to "Telefon je položen ravno.",
            "alert_ws_disconnected" to "Nema veze sa serverom."
        )
    )
}
