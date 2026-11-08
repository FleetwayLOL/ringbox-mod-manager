package app.ringbox.ui

/**
 * Языки интерфейса. AUTO = брать язык системы, регион не важен.
 * Набор как в соседней проге юзера: славянские, тюркские, CJK, RTL.
 */
enum class Lang(val code: String, val badge: String, val native: String, val english: String) {
    AUTO("auto", "AUTO", "Автоопределение", "Язык системы"),
    RU("ru", "RU", "Русский", "Russian"),
    EN("en", "EN", "English", "International"),
    BE("be", "BE", "Беларуская", "Belarusian"),
    KK("kk", "KK", "Қазақша", "Kazakh"),
    DE("de", "DE", "Deutsch", "German"),
    FR("fr", "FR", "Français", "French"),
    ES("es", "ES", "Español", "Spanish"),
    PT("pt", "PT", "Português", "Portuguese"),
    IT("it", "IT", "Italiano", "Italian"),
    PL("pl", "PL", "Polski", "Polish"),
    TR("tr", "TR", "Türkçe", "Turkish"),
    ZH("zh", "ZH", "简体中文", "Chinese"),
    JA("ja", "JA", "日本語", "Japanese"),
    KO("ko", "KO", "한국어", "Korean"),
    AR("ar", "AR", "العربية", "Arabic"),
    ;

    companion object {
        fun fromCode(code: String?): Lang = entries.firstOrNull { it.code == code } ?: AUTO

        /** Язык системы, если он есть в списке, иначе русский. */
        fun system(): Lang {
            val tag = java.util.Locale.getDefault().language
            return entries.firstOrNull { it.code == tag } ?: RU
        }

        fun resolve(saved: Lang): Lang = if (saved == AUTO) system() else saved
    }
}

/** Строки интерфейса. Ключ один — значение на всех языках. */
data class S(
    val catalog: String,
    val mine: String,
    val folder: String,
    val settings: String,
    val search: String,
    val install: String,
    val downloadInstall: String,
    val fromFile: String,
    val findFolder: String,
    val grantAccess: String,
    val language: String,
    val bypass: String,
)

private val RU = S(
    "Каталог", "Мои моды", "Папка", "Настройки", "Поиск модов", "Установить",
    "Скачать и установить", "Установить из файла", "Найти папку игры",
    "Дать доступ к файлам", "Язык", "Обход блокировки"
)

fun strings(lang: Lang): S = when (Lang.resolve(lang)) {
    Lang.BE -> S(
        "Каталог", "Мае моды", "Папка", "Налады", "Пошук модаў", "Усталяваць",
        "Спампаваць і ўсталяваць", "Усталяваць з файла", "Знайсці папку гульні",
        "Дазволіць доступ да файлаў", "Мова", "Абыход блакіроўкі"
    )
    Lang.KK -> S(
        "Каталог", "Менің модтарым", "Қалта", "Параметрлер", "Модтарды іздеу", "Орнату",
        "Жүктеп орнату", "Файлдан орнату", "Ойын қалтасын табу",
        "Файлдарға рұқсат беру", "Тіл", "Блоктауды айналып өту"
    )
    Lang.DE -> S(
        "Katalog", "Meine Mods", "Ordner", "Einstellungen", "Mods suchen", "Installieren",
        "Herunterladen & installieren", "Aus Datei installieren", "Spielordner finden",
        "Dateizugriff erlauben", "Sprache", "Sperre umgehen"
    )
    Lang.FR -> S(
        "Catalogue", "Mes mods", "Dossier", "Paramètres", "Rechercher des mods", "Installer",
        "Télécharger et installer", "Installer depuis un fichier", "Trouver le dossier du jeu",
        "Autoriser l'accès aux fichiers", "Langue", "Contourner le blocage"
    )
    Lang.ES -> S(
        "Catálogo", "Mis mods", "Carpeta", "Ajustes", "Buscar mods", "Instalar",
        "Descargar e instalar", "Instalar desde archivo", "Buscar la carpeta del juego",
        "Permitir acceso a archivos", "Idioma", "Saltar el bloqueo"
    )
    Lang.PT -> S(
        "Catálogo", "Meus mods", "Pasta", "Configurações", "Buscar mods", "Instalar",
        "Baixar e instalar", "Instalar de arquivo", "Encontrar a pasta do jogo",
        "Permitir acesso aos arquivos", "Idioma", "Contornar bloqueio"
    )
    Lang.IT -> S(
        "Catalogo", "I miei mod", "Cartella", "Impostazioni", "Cerca mod", "Installa",
        "Scarica e installa", "Installa da file", "Trova la cartella del gioco",
        "Consenti accesso ai file", "Lingua", "Eludere il blocco"
    )
    Lang.PL -> S(
        "Katalog", "Moje mody", "Folder", "Ustawienia", "Szukaj modów", "Zainstaluj",
        "Pobierz i zainstaluj", "Zainstaluj z pliku", "Znajdź folder gry",
        "Zezwól na dostęp do plików", "Język", "Obejście blokady"
    )
    Lang.TR -> S(
        "Katalog", "Modlarım", "Klasör", "Ayarlar", "Mod ara", "Yükle",
        "İndir ve yükle", "Dosyadan yükle", "Oyun klasörünü bul",
        "Dosya erişimine izin ver", "Dil", "Engeli aş"
    )
    Lang.ZH -> S(
        "目录", "我的模组", "文件夹", "设置", "搜索模组", "安装",
        "下载并安装", "从文件安装", "查找游戏文件夹", "允许访问文件", "语言", "绕过封锁"
    )
    Lang.JA -> S(
        "カタログ", "マイMOD", "フォルダ", "設定", "MODを検索", "インストール",
        "ダウンロードしてインストール", "ファイルからインストール", "ゲームフォルダを探す",
        "ファイルアクセスを許可", "言語", "ブロック回避"
    )
    Lang.KO -> S(
        "카탈로그", "내 모드", "폴더", "설정", "모드 검색", "설치",
        "다운로드 및 설치", "파일에서 설치", "게임 폴더 찾기", "파일 접근 허용", "언어", "차단 우회"
    )
    Lang.AR -> S(
        "الكتالوج", "موداتي", "المجلد", "الإعدادات", "البحث عن المودات", "تثبيت",
        "تنزيل وتثبيت", "تثبيت من ملف", "إيجاد مجلد اللعبة", "السماح بالوصول إلى الملفات",
        "اللغة", "تجاوز الحجب"
    )
    Lang.EN -> S(
        "Catalog", "My mods", "Folder", "Settings", "Search mods", "Install",
        "Download & install", "Install from file", "Find game folder",
        "Grant file access", "Language", "Block bypass"
    )
    else -> RU
}
