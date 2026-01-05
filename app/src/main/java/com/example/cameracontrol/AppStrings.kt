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
    PT_BR("pt-BR", "Português (Brasil)"),
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
            val normalized = code.lowercase()
            return values().firstOrNull { it.code.lowercase() == normalized }
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
            "pt" -> if (locale.country.equals("BR", ignoreCase = true)) {
                AppLanguage.PT_BR
            } else {
                AppLanguage.PT
            }
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
            "intro_main" to "Use players' phones as cameras. The game is recorded continuously, " +
                "and the best or controversial moments are saved with one tap and available " +
                "for review and analysis from different angles.",
            "ok" to "OK",
            "consent_title" to "Before you start",
            "consent_body" to "This app records video and audio using the camera and microphone. " +
                "Recording continues even when the screen is off.",
            "consent_ack" to "I understand and agree",
            "consent_continue" to "Continue",
            "room_id_title" to "Room ID",
            "room_id_prompt" to "Enter the Room ID for this session:",
            "permissions_required" to "Permissions required!",
            "camera_permissions_msg" to "Please grant camera and audio permissions to continue.",
            "starting_camera" to "Starting camera service...",
            "notification_title" to "Recording video and audio in background",
            "notification_text" to "Recording continues with screen off.",
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
            "intro_main" to "使用球员的手机作为摄像机。比赛将持续录制，精彩或有争议的瞬间可一键保存，并可从不同角度观看和分析。",
            "ok" to "确定",
            "consent_title" to "开始前",
            "consent_body" to "此应用使用摄像头和麦克风录制视频和音频。屏幕关闭时也会继续录制。",
            "consent_ack" to "我已了解并同意",
            "consent_continue" to "继续",
            "room_id_title" to "房间 ID",
            "room_id_prompt" to "请输入本次会话的房间 ID：",
            "permissions_required" to "需要权限！",
            "camera_permissions_msg" to "请授予相机和音频权限以继续。",
            "starting_camera" to "正在启动相机服务...",
            "notification_title" to "正在后台录制视频和音频",
            "notification_text" to "屏幕关闭时仍会继续录制。",
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
            "intro_main" to "Usa los teléfonos de los jugadores como cámaras. El partido se graba continuamente, y los mejores o más discutidos momentos se guardan con un toque y están disponibles para revisar y analizar desde distintos ángulos.",
            "ok" to "OK",
            "consent_title" to "Antes de empezar",
            "consent_body" to "Esta app graba video y audio usando la cámara y el micrófono. La grabación continúa incluso con la pantalla apagada.",
            "consent_ack" to "Entiendo y acepto",
            "consent_continue" to "Continuar",
            "room_id_title" to "ID de sala",
            "room_id_prompt" to "Introduce el ID de la sala para esta sesión:",
            "permissions_required" to "¡Se requieren permisos!",
            "camera_permissions_msg" to "Concede permisos de cámara y audio para continuar.",
            "starting_camera" to "Iniciando servicio de cámara...",
            "notification_title" to "Grabando video y audio en segundo plano",
            "notification_text" to "La grabación continúa con la pantalla apagada.",
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
            "intro_main" to "استخدم هواتف اللاعبين ككاميرات. تُسجَّل المباراة باستمرار، ويتم حفظ أفضل أو أكثر اللحظات جدلاً بضغطة واحدة وتتاح للمراجعة والتحليل من زوايا مختلفة.",
            "ok" to "حسنًا",
            "consent_title" to "قبل البدء",
            "consent_body" to "يسجل هذا التطبيق الفيديو والصوت باستخدام الكاميرا والميكروفون. يستمر التسجيل حتى مع إيقاف تشغيل الشاشة.",
            "consent_ack" to "أفهم وأوافق",
            "consent_continue" to "متابعة",
            "room_id_title" to "معرّف الغرفة",
            "room_id_prompt" to "أدخل معرّف الغرفة لهذه الجلسة:",
            "permissions_required" to "الأذونات مطلوبة!",
            "camera_permissions_msg" to "يرجى منح أذونات الكاميرا والصوت للمتابعة.",
            "starting_camera" to "بدء خدمة الكاميرا...",
            "notification_title" to "يتم تسجيل الفيديو والصوت في الخلفية",
            "notification_text" to "يستمر التسجيل عند إيقاف تشغيل الشاشة.",
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
            "intro_main" to "खिलाड़ियों के फ़ोन को कैमरों के रूप में इस्तेमाल करें। खेल लगातार रिकॉर्ड होता है, और बेहतरीन या विवादित पल एक टैप से सेव होकर अलग-अलग कोणों से देखने और विश्लेषण के लिए उपलब्ध होते हैं।",
            "ok" to "ठीक है",
            "consent_title" to "शुरू करने से पहले",
            "consent_body" to "यह ऐप कैमरा और माइक्रोफ़ोन का उपयोग करके वीडियो और ऑडियो रिकॉर्ड करता है। स्क्रीन बंद होने पर भी रिकॉर्डिंग जारी रहती है।",
            "consent_ack" to "मैं समझता/समझती हूँ और सहमत हूँ",
            "consent_continue" to "जारी रखें",
            "room_id_title" to "रूम आईडी",
            "room_id_prompt" to "इस सत्र के लिए रूम आईडी दर्ज करें:",
            "permissions_required" to "अनुमतियाँ आवश्यक हैं!",
            "camera_permissions_msg" to "जारी रखने के लिए कैमरा और ऑडियो अनुमतियाँ दें।",
            "starting_camera" to "कैमरा सेवा शुरू हो रही है...",
            "notification_title" to "बैकग्राउंड में वीडियो और ऑडियो रिकॉर्ड हो रहा है",
            "notification_text" to "स्क्रीन बंद होने पर भी रिकॉर्डिंग जारी रहती है।",
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
            "intro_main" to "খেলোয়াড়দের ফোনকে ক্যামেরা হিসেবে ব্যবহার করুন। খেলা ধারাবাহিকভাবে রেকর্ড হয়, আর সেরা বা বিতর্কিত মুহূর্তগুলো এক ট্যাপে সংরক্ষণ হয়ে বিভিন্ন কোণ থেকে দেখা ও বিশ্লেষণের জন্য উপলব্ধ থাকে।",
            "ok" to "ঠিক আছে",
            "consent_title" to "শুরু করার আগে",
            "consent_body" to "এই অ্যাপটি ক্যামেরা ও মাইক্রোফোন ব্যবহার করে ভিডিও ও অডিও রেকর্ড করে। স্ক্রিন বন্ধ থাকলেও রেকর্ডিং চলতে থাকে।",
            "consent_ack" to "আমি বুঝেছি এবং সম্মত",
            "consent_continue" to "চালিয়ে যান",
            "room_id_title" to "রুম আইডি",
            "room_id_prompt" to "এই সেশনের জন্য রুম আইডি দিন:",
            "permissions_required" to "অনুমতি প্রয়োজন!",
            "camera_permissions_msg" to "চালিয়ে যেতে ক্যামেরা ও অডিও অনুমতি দিন।",
            "starting_camera" to "ক্যামেরা সার্ভিস চালু হচ্ছে...",
            "notification_title" to "ব্যাকগ্রাউন্ডে ভিডিও ও অডিও রেকর্ড হচ্ছে",
            "notification_text" to "স্ক্রিন বন্ধ থাকলেও রেকর্ডিং চলতে থাকে।",
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
            "intro_main" to "선수들의 휴대폰을 카메라로 사용하세요. 경기는 계속 녹화되며, 최고의 순간이나 논란이 되는 순간을 한 번의 터치로 저장해 다양한 각도에서 확인하고 분석할 수 있습니다.",
            "ok" to "확인",
            "consent_title" to "시작하기 전에",
            "consent_body" to "이 앱은 카메라와 마이크를 사용해 영상과 음성을 녹화합니다. 화면이 꺼져도 녹화가 계속됩니다.",
            "consent_ack" to "이해했고 동의합니다",
            "consent_continue" to "계속",
            "room_id_title" to "룸 ID",
            "room_id_prompt" to "이 세션의 룸 ID를 입력하세요:",
            "permissions_required" to "권한이 필요합니다!",
            "camera_permissions_msg" to "계속하려면 카메라 및 오디오 권한을 허용하세요.",
            "starting_camera" to "카메라 서비스를 시작하는 중...",
            "notification_title" to "백그라운드에서 영상과 음성을 녹화 중",
            "notification_text" to "화면이 꺼져도 녹화가 계속됩니다.",
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
            "intro_main" to "Gunakan ponsel pemain sebagai kamera. Pertandingan direkam terus-menerus, dan momen terbaik atau kontroversial dapat disimpan dengan satu ketukan serta tersedia untuk ditinjau dan dianalisis dari berbagai sudut.",
            "ok" to "OK",
            "consent_title" to "Sebelum mulai",
            "consent_body" to "Aplikasi ini merekam video dan audio menggunakan kamera dan mikrofon. Perekaman tetap berjalan saat layar mati.",
            "consent_ack" to "Saya paham dan setuju",
            "consent_continue" to "Lanjutkan",
            "room_id_title" to "ID Ruangan",
            "room_id_prompt" to "Masukkan ID ruangan untuk sesi ini:",
            "permissions_required" to "Izin diperlukan!",
            "camera_permissions_msg" to "Berikan izin kamera dan audio untuk melanjutkan.",
            "starting_camera" to "Memulai layanan kamera...",
            "notification_title" to "Merekam video dan audio di latar belakang",
            "notification_text" to "Perekaman tetap berjalan saat layar mati.",
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
            "intro_main" to "Use os telefones dos jogadores como câmeras. O jogo é gravado continuamente, e os melhores ou mais polêmicos momentos são salvos com um toque e ficam disponíveis para revisão e análise de diferentes ângulos.",
            "ok" to "OK",
            "consent_title" to "Antes de começar",
            "consent_body" to "Este app grava vídeo e áudio usando a câmera e o microfone. A gravação continua mesmo com a tela desligada.",
            "consent_ack" to "Eu entendo e concordo",
            "consent_continue" to "Continuar",
            "room_id_title" to "ID da sala",
            "room_id_prompt" to "Digite o ID da sala para esta sessão:",
            "permissions_required" to "Permissões necessárias!",
            "camera_permissions_msg" to "Conceda permissões de câmera e áudio para continuar.",
            "starting_camera" to "Iniciando serviço da câmera...",
            "notification_title" to "Gravando vídeo e áudio em segundo plano",
            "notification_text" to "A gravação continua com a tela desligada.",
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
        AppLanguage.PT_BR to mapOf(
            "welcome" to "Bem-vindo",
            "intro_text" to "Grave momentos incríveis de vôlei com seu telefone. " +
                "Para economizar bateria, a gravação pode continuar com a tela apagada. " +
                "Pare a gravação ao terminar.",
            "intro_main" to "Use os celulares dos jogadores como câmeras. O jogo é gravado continuamente, e os melhores ou mais polêmicos momentos são salvos com um toque e ficam disponíveis para revisão e análise de diferentes ângulos.",
            "ok" to "OK",
            "consent_title" to "Antes de começar",
            "consent_body" to "Este app grava vídeo e áudio usando a câmera e o microfone. A gravação continua mesmo com a tela apagada.",
            "consent_ack" to "Eu entendo e concordo",
            "consent_continue" to "Continuar",
            "room_id_title" to "ID da sala",
            "room_id_prompt" to "Digite o ID da sala para esta sessão:",
            "permissions_required" to "Permissões necessárias!",
            "camera_permissions_msg" to "Conceda permissões de câmera e áudio para continuar.",
            "starting_camera" to "Iniciando serviço da câmera...",
            "notification_title" to "Gravando vídeo e áudio em segundo plano",
            "notification_text" to "A gravação continua com a tela apagada.",
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
            "intro_main" to "Utilisez les téléphones des joueurs comme caméras. Le match est enregistré en continu, et les meilleurs ou plus discutés moments sont sauvegardés d’un tap et disponibles pour visionnage et analyse sous différents angles.",
            "ok" to "OK",
            "consent_title" to "Avant de commencer",
            "consent_body" to "Cette application enregistre la vidéo et l’audio avec la caméra et le micro. L’enregistrement continue même écran éteint.",
            "consent_ack" to "J’ai compris et j’accepte",
            "consent_continue" to "Continuer",
            "room_id_title" to "ID de la salle",
            "room_id_prompt" to "Saisissez l'ID de la salle pour cette session :",
            "permissions_required" to "Autorisations requises !",
            "camera_permissions_msg" to "Accordez les autorisations caméra et audio pour continuer.",
            "starting_camera" to "Démarrage du service caméra...",
            "notification_title" to "Enregistrement vidéo et audio en arrière-plan",
            "notification_text" to "L’enregistrement continue écran éteint.",
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
            "intro_main" to "Usa i telefoni dei giocatori come telecamere. La partita viene registrata continuamente e i momenti migliori o più discussi si salvano con un tocco e sono disponibili per la revisione e l’analisi da diverse angolazioni.",
            "ok" to "OK",
            "consent_title" to "Prima di iniziare",
            "consent_body" to "Questa app registra video e audio usando fotocamera e microfono. La registrazione continua anche con lo schermo spento.",
            "consent_ack" to "Ho capito e accetto",
            "consent_continue" to "Continua",
            "room_id_title" to "ID stanza",
            "room_id_prompt" to "Inserisci l'ID stanza per questa sessione:",
            "permissions_required" to "Permessi richiesti!",
            "camera_permissions_msg" to "Concedi i permessi per fotocamera e audio per continuare.",
            "starting_camera" to "Avvio del servizio fotocamera...",
            "notification_title" to "Registrazione di video e audio in background",
            "notification_text" to "La registrazione continua con lo schermo spento.",
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
            "intro_main" to "選手のスマホをカメラとして使います。試合は常時録画され、最高の瞬間や議論のある場面はワンタップで保存され、さまざまな角度から確認・分析できます。",
            "ok" to "OK",
            "consent_title" to "開始前に",
            "consent_body" to "このアプリはカメラとマイクを使って動画と音声を録画します。画面がオフでも録画は続きます。",
            "consent_ack" to "理解し、同意します",
            "consent_continue" to "続行",
            "room_id_title" to "ルーム ID",
            "room_id_prompt" to "このセッションのルーム ID を入力してください：",
            "permissions_required" to "権限が必要です！",
            "camera_permissions_msg" to "続行するにはカメラと音声の権限を付与してください。",
            "starting_camera" to "カメラサービスを開始しています...",
            "notification_title" to "バックグラウンドで動画と音声を録画中",
            "notification_text" to "画面がオフでも録画は続きます。",
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
            "intro_main" to "Oyuncuların telefonlarını kamera olarak kullanın. Maç sürekli kaydedilir; en iyi veya tartışmalı anlar tek dokunuşla kaydedilir ve farklı açılardan izleme ve analiz için kullanılabilir.",
            "ok" to "Tamam",
            "consent_title" to "Başlamadan önce",
            "consent_body" to "Bu uygulama kamera ve mikrofonu kullanarak video ve ses kaydeder. Ekran kapalıyken de kayıt devam eder.",
            "consent_ack" to "Anladım ve kabul ediyorum",
            "consent_continue" to "Devam",
            "room_id_title" to "Oda ID",
            "room_id_prompt" to "Bu oturum için oda ID'sini girin:",
            "permissions_required" to "İzinler gerekli!",
            "camera_permissions_msg" to "Devam etmek için kamera ve ses izinleri verin.",
            "starting_camera" to "Kamera hizmeti başlatılıyor...",
            "notification_title" to "Arka planda video ve ses kaydı yapılıyor",
            "notification_text" to "Ekran kapalıyken de kayıt devam eder.",
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
            "intro_main" to "Sử dụng điện thoại của người chơi làm camera. Trận đấu được ghi liên tục, và những khoảnh khắc hay hoặc gây tranh cãi được lưu chỉ với một chạm, có thể xem và phân tích từ nhiều góc độ.",
            "ok" to "OK",
            "consent_title" to "Trước khi bắt đầu",
            "consent_body" to "Ứng dụng này ghi lại video và âm thanh bằng camera và micrô. Ghi hình vẫn tiếp tục khi tắt màn hình.",
            "consent_ack" to "Tôi hiểu và đồng ý",
            "consent_continue" to "Tiếp tục",
            "room_id_title" to "ID phòng",
            "room_id_prompt" to "Nhập ID phòng cho phiên này:",
            "permissions_required" to "Cần cấp quyền!",
            "camera_permissions_msg" to "Vui lòng cấp quyền camera và âm thanh để tiếp tục.",
            "starting_camera" to "Đang khởi động dịch vụ camera...",
            "notification_title" to "Đang ghi video và âm thanh trong nền",
            "notification_text" to "Ghi hình vẫn tiếp tục khi tắt màn hình.",
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
            "intro_main" to "ใช้โทรศัพท์ของผู้เล่นเป็นกล้อง เกมจะถูกบันทึกต่อเนื่อง และช่วงเวลาที่ดีที่สุดหรือมีข้อโต้แย้งจะถูกบันทึกด้วยการแตะครั้งเดียว พร้อมให้รับชมและวิเคราะห์จากหลายมุมมอง.",
            "ok" to "ตกลง",
            "consent_title" to "ก่อนเริ่มต้น",
            "consent_body" to "แอปนี้บันทึกวิดีโอและเสียงด้วยกล้องและไมโครโฟน การบันทึกจะดำเนินต่อแม้หน้าจอดับ",
            "consent_ack" to "ฉันเข้าใจและยอมรับ",
            "consent_continue" to "ดำเนินการต่อ",
            "room_id_title" to "รหัสห้อง",
            "room_id_prompt" to "กรอกรหัสห้องสำหรับเซสชันนี้:",
            "permissions_required" to "ต้องขออนุญาต!",
            "camera_permissions_msg" to "โปรดอนุญาตการเข้าถึงกล้องและเสียงเพื่อดำเนินการต่อ",
            "starting_camera" to "กำลังเริ่มบริการกล้อง...",
            "notification_title" to "กำลังบันทึกวิดีโอและเสียงในพื้นหลัง",
            "notification_text" to "การบันทึกยังคงดำเนินต่อเมื่อปิดหน้าจอ.",
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
            "intro_main" to "Используйте телефоны игроков как камеры. " +
                "Игра записывается постоянно, а лучшие и спорные моменты сохраняются " +
                "одним нажатием кнопки и доступны для просмотра и анализа с разных углов.",
            "ok" to "ОК",
            "consent_title" to "Перед началом",
            "consent_body" to "Приложение записывает видео и аудио, используя камеру и микрофон. " +
                "Запись продолжается даже при выключенном экране.",
            "consent_ack" to "Я понимаю и согласен(на)",
            "consent_continue" to "Продолжить",
            "room_id_title" to "ID комнаты",
            "room_id_prompt" to "Введите ID комнаты для этой сессии:",
            "permissions_required" to "Требуются разрешения!",
            "camera_permissions_msg" to "Пожалуйста, предоставьте разрешения на камеру и аудио.",
            "starting_camera" to "Запуск сервиса камеры...",
            "notification_title" to "Идёт запись видео и аудио в фоне",
            "notification_text" to "Запись продолжается при выключенном экране.",
            "debug_logs" to "Логи отладки:",
            "simulate_save" to "Сохранить запись",
            "zoom" to "Зум",
            "exit" to "Выход",
            "privacy_disclaimer" to "Нажимая ОК, вы принимаете нашу",
            "privacy_link" to "Политику конфиденциальности",
            "language_label" to "Язык",
            "website_label" to "Сайт",
            "alert_phone_flat" to "Телефон лежит плашмя.",
            "alert_ws_disconnected" to "Нет подключения к серверу."
        ),
        AppLanguage.DE to mapOf(
            "welcome" to "Willkommen",
            "intro_text" to "Nimm schöne Volleyball-Momente mit deinem Handy auf. " +
                "Um Akku zu sparen, kann die Aufnahme bei ausgeschaltetem Bildschirm weiterlaufen. " +
                "Beende die Aufnahme, wenn du fertig bist.",
            "intro_main" to "Nutze die Handys der Spieler als Kameras. Das Spiel wird kontinuierlich aufgezeichnet, und die besten oder umstrittenen Momente werden mit einem Tipp gespeichert und sind zur Ansicht und Analyse aus verschiedenen Blickwinkeln verfügbar.",
            "ok" to "OK",
            "consent_title" to "Vor dem Start",
            "consent_body" to "Diese App zeichnet Video und Audio mit Kamera und Mikrofon auf. Die Aufnahme läuft auch bei ausgeschaltetem Bildschirm weiter.",
            "consent_ack" to "Ich habe verstanden und stimme zu",
            "consent_continue" to "Weiter",
            "room_id_title" to "Raum-ID",
            "room_id_prompt" to "Gib die Raum-ID für diese Sitzung ein:",
            "permissions_required" to "Berechtigungen erforderlich!",
            "camera_permissions_msg" to "Bitte Kamera- und Audio-Berechtigungen erteilen.",
            "starting_camera" to "Kamera-Service wird gestartet...",
            "notification_title" to "Video- und Audioaufnahme im Hintergrund",
            "notification_text" to "Die Aufnahme läuft bei ausgeschaltetem Bildschirm weiter.",
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
            "intro_main" to "Użyj telefonów zawodników jako kamer. Mecz jest nagrywany na bieżąco, a najlepsze lub najbardziej dyskusyjne momenty są zapisywane jednym dotknięciem i dostępne do oglądania oraz analizy z różnych ujęć.",
            "ok" to "OK",
            "consent_title" to "Przed rozpoczęciem",
            "consent_body" to "Ta aplikacja nagrywa wideo i audio przy użyciu aparatu i mikrofonu. Nagrywanie trwa również przy wyłączonym ekranie.",
            "consent_ack" to "Rozumiem i wyrażam zgodę",
            "consent_continue" to "Kontynuuj",
            "room_id_title" to "ID pokoju",
            "room_id_prompt" to "Wpisz ID pokoju dla tej sesji:",
            "permissions_required" to "Wymagane uprawnienia!",
            "camera_permissions_msg" to "Aby kontynuować, przyznaj uprawnienia kamery i dźwięku.",
            "starting_camera" to "Uruchamianie usługi kamery...",
            "notification_title" to "Nagrywanie wideo i audio w tle",
            "notification_text" to "Nagrywanie trwa także przy wyłączonym ekranie.",
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
            "intro_main" to "Gebruik de telefoons van spelers als camera’s. De wedstrijd wordt continu opgenomen en de beste of meest omstreden momenten worden met één tik opgeslagen en zijn beschikbaar voor bekijken en analyseren vanuit verschillende hoeken.",
            "ok" to "OK",
            "consent_title" to "Voordat je begint",
            "consent_body" to "Deze app neemt video en audio op met de camera en microfoon. Opname gaat door, ook als het scherm uit is.",
            "consent_ack" to "Ik begrijp het en ga akkoord",
            "consent_continue" to "Doorgaan",
            "room_id_title" to "Kamer-ID",
            "room_id_prompt" to "Voer de kamer-ID voor deze sessie in:",
            "permissions_required" to "Machtigingen vereist!",
            "camera_permissions_msg" to "Geef camera- en audiomachtigingen om door te gaan.",
            "starting_camera" to "Cameraservice starten...",
            "notification_title" to "Video en audio opnemen op de achtergrond",
            "notification_text" to "Opname gaat door als het scherm uit is.",
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
            "intro_main" to "کھلاڑیوں کے فونز کو کیمروں کے طور پر استعمال کریں۔ میچ مسلسل ریکارڈ ہوتا ہے، اور بہترین یا متنازع لمحات ایک ٹیپ سے محفوظ ہو جاتے ہیں اور مختلف زاویوں سے دیکھنے اور تجزیہ کے لیے دستیاب ہوتے ہیں۔",
            "ok" to "ٹھیک ہے",
            "consent_title" to "شروع کرنے سے پہلے",
            "consent_body" to "یہ ایپ کیمرہ اور مائیکروفون استعمال کرتے ہوئے ویڈیو اور آڈیو ریکارڈ کرتی ہے۔ اسکرین بند ہونے پر بھی ریکارڈنگ جاری رہتی ہے۔",
            "consent_ack" to "میں سمجھتا/سمجھتی ہوں اور متفق ہوں",
            "consent_continue" to "جاری رکھیں",
            "room_id_title" to "روم آئی ڈی",
            "room_id_prompt" to "اس سیشن کے لیے روم آئی ڈی درج کریں:",
            "permissions_required" to "اجازتیں درکار ہیں!",
            "camera_permissions_msg" to "جاری رکھنے کے لیے کیمرا اور آڈیو اجازتیں دیں۔",
            "starting_camera" to "کیمرہ سروس شروع ہو رہی ہے...",
            "notification_title" to "بیک گراؤنڈ میں ویڈیو اور آڈیو ریکارڈ ہو رہا ہے",
            "notification_text" to "اسکرین بند ہونے پر بھی ریکارڈنگ جاری رہتی ہے۔",
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
            "intro_main" to "از تلفن‌های بازیکنان به‌عنوان دوربین استفاده کنید. بازی به‌صورت پیوسته ضبط می‌شود و بهترین یا بحث‌برانگیزترین لحظات با یک لمس ذخیره شده و برای بازبینی و تحلیل از زوایای مختلف در دسترس است.",
            "ok" to "باشه",
            "consent_title" to "قبل از شروع",
            "consent_body" to "این برنامه با استفاده از دوربین و میکروفون، ویدیو و صدا را ضبط می‌کند. ضبط حتی با خاموش بودن صفحه ادامه دارد.",
            "consent_ack" to "متوجه شدم و موافقم",
            "consent_continue" to "ادامه",
            "room_id_title" to "شناسه اتاق",
            "room_id_prompt" to "شناسه اتاق این جلسه را وارد کنید:",
            "permissions_required" to "مجوزها لازم است!",
            "camera_permissions_msg" to "برای ادامه، مجوز دوربین و صدا را بدهید.",
            "starting_camera" to "در حال شروع سرویس دوربین...",
            "notification_title" to "ضبط ویدیو و صدا در پس‌زمینه",
            "notification_text" to "ضبط حتی با خاموش بودن صفحه ادامه دارد.",
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
            "intro_main" to "Koristite telefone igrača kao kamere. Utakmica se snima neprekidno, a najbolji ili sporni momenti se čuvaju jednim dodirom i dostupni su za pregled i analizu iz različitih uglova.",
            "ok" to "U redu",
            "consent_title" to "Pre početka",
            "consent_body" to "Ova aplikacija snima video i audio pomoću kamere i mikrofona. Snimanje se nastavlja i kada je ekran ugašen.",
            "consent_ack" to "Razumem i slažem se",
            "consent_continue" to "Nastavi",
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
