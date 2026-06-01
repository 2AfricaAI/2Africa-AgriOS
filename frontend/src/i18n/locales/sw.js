/**
 * Swahili (Kiswahili) - sparse translation covering the mobile worker UI.
 * Untranslated keys fall back to English via i18n's fallbackLocale.
 */
export default {
  brand: '2Africa AgriOS',
  brandFull: '2Africa AgriOS by 2Africa.AI',

  // ---------- Auth / Login ----------
  auth: {
    login: 'Ingia',
    loginTitle: 'Ingia kwenye {brand}',
    username: 'Jina la mtumiaji',
    password: 'Nenosiri',
    usernamePlaceholder: 'Ingiza jina la mtumiaji',
    passwordPlaceholder: 'Ingiza nenosiri',
    loginSuccess: 'Umeingia',
    loginFailed: 'Kuingia kumeshindwa',
    usernameRequired: 'Jina la mtumiaji linahitajika',
    passwordRequired: 'Nenosiri linahitajika',
    role: 'Cheo',
  },

  // ---------- Generic buttons / labels ----------
  common: {
    yes: 'Ndio',
    no: 'Hapana',
    ok: 'Sawa',
    cancel: 'Ghairi',
    confirm: 'Thibitisha',
    save: 'Hifadhi',
    submit: 'Wasilisha',
    create: 'Tengeneza',
    edit: 'Hariri',
    delete: 'Futa',
    view: 'Tazama',
    search: 'Tafuta',
    reset: 'Anza upya',
    refresh: 'Onyesha upya',
    back: 'Rudi',
    next: 'Endelea',
    add: 'Ongeza',
    remove: 'Toa',
    close: 'Funga',
    loading: 'Inapakia...',
    empty: 'Hakuna data',
    actions: 'Vitendo',
    operationSuccess: 'Imefanikiwa',
    operationFailed: 'Imeshindwa',
    tip: 'Kidokezo',
    warning: 'Onyo',
    error: 'Kosa',
    welcome: 'Karibu',
    logout: 'Toka',
    remark: 'Maelezo',
  },

  // ---------- Validation ----------
  valid: {
    required: '{field} inahitajika',
    mustBePositive: '{field} lazima iwe > 0',
  },

  // Sprint 41: AgriOS-native Customer Service workspace
  menu: {
    customerService: 'Huduma kwa Wateja',
  },
  service: {
    title: 'Eneo la Huduma kwa Wateja',
    reload: 'Onyesha upya',

    // list page
    filters: 'Vichujio',
    status: 'Hali',
    statusOpen: 'Wazi',
    statusPending: 'Inasubiri',
    statusResolved: 'Imetatuliwa',
    statusSnoozed: 'Imeahirishwa',
    assignee: 'Mhudumu',
    assigneeAll: 'Wote',
    assigneeMe: 'Mimi',
    assigneeUnassigned: 'Hawajapangiwa',
    inbox: 'Sanduku',
    inboxAll: 'Masanduku yote',
    conversations: 'Mazungumzo',
    conversationsUnit: '',
    empty: 'Hakuna mazungumzo yanayolingana na vichujio',
    unknownContact: 'Mteja asiyejulikana',
    selectAConversation: 'Chagua mazungumzo kushoto kuona maelezo',
    loadFailed: 'Imeshindwa kupakia',

    // detail page
    markResolved: 'Tia alama imetatuliwa',
    reopen: 'Fungua tena',
    creditLevel: 'Kiwango cha mkopo',
    paymentTerms: 'Masharti ya malipo',
    openOrders: 'Maagizo wazi',
    overdueAr: 'Madeni yaliyochelewa',
    openComplaints: 'Malalamiko wazi',

    // composer
    reply: 'Jibu',
    privateNote: 'Kumbukumbu ya ndani',
    replyHint: 'Mteja atauona — pitia kabla ya kutuma',
    privateNoteHint: 'Inaonekana kwa timu yako tu',
    replyPlaceholder: 'Andika jibu lako (Ctrl/Cmd + Enter kutuma)',
    privateNotePlaceholder: 'Andika kumbukumbu ya timu',
    sendReply: 'Tuma jibu',
    saveNote: 'Hifadhi kumbukumbu',
    replySent: 'Jibu limetumwa',
    noteSaved: 'Kumbukumbu imehifadhiwa',
    sendFailed: 'Imeshindwa kutuma',

    // AI
    aiDraft: 'Pendekezo la AI',
    aiThinking: 'AI inafikiria...',
    aiFailed: 'Wito wa AI umeshindwa',

    // status
    statusChanged: 'Hali imebadilishwa',
    statusFailed: 'Imeshindwa kubadilisha hali',

    // Sprint 47: Sera ya WhatsApp bila gharama
    whatsAppWindowRemaining: 'Dirisha la huduma: salio {h}h {m}m',
    whatsAppWindowExpired: 'Dirisha la huduma limeisha',
    whatsAppNoWindow: 'Tunasubiri ujumbe wa kwanza wa mteja',
    whatsAppWindowTooltip: 'Sera ya Meta: majibu ya umma ni bure kwa masaa 24 baada ya ujumbe wa mteja.',
    whatsAppExpiredTooltip: 'Nje ya dirisha la masaa 24. Jibu la umma lingehitaji template ya kulipia; limezuiwa na sera ya gharama-sifuri. Tumia maelezo ya ndani au fuata kwa SMS.',
    whatsAppBlockedTooltip: 'Jibu la umma linahitaji template ya kulipia; limezuiwa na sera ya gharama-sifuri',
    whatsAppBlockedBanner: 'Dirisha la masaa 24 la WhatsApp limeisha. Jibu la umma lingehitaji template ya kulipia ya Meta na limezuiwa na sera yako ya gharama-sifuri. Bado unaweza kuongeza maelezo ya ndani au kufikia mteja kupitia SMS / Email.',

    // Sprint 42: Inbox setup wizard (Swahili short labels — falls back to en)
    inboxesTitle: 'Masanduku ya Wateja',
    inboxesSub: 'Unganisha Barua-pepe / WhatsApp / Soga ya tovuti — ujumbe wote unakuja hapa',
    addInbox: 'Ongeza sanduku',
    noInboxes: 'Bado hakuna sanduku — bofya kuongeza la kwanza',
    openInChatwoot: 'Fungua katika Chatwoot',
    deleteInbox: 'Futa sanduku',
    deleteConfirm: 'Futa sanduku "{name}"? Historia ya mazungumzo itafutwa.',
    inboxCreated: 'Sanduku "{name}" limeundwa',
    inboxDeleted: 'Sanduku limefutwa',
    deleteFailed: 'Imeshindwa kufuta',
    createFailed: 'Imeshindwa kuunda',
    createInbox: 'Unda sanduku',
    wizardTitle: 'Ongeza Njia ya Huduma',
    pickChannelHint: 'Chagua njia ya kuunganisha.',
    comingSoon: 'Inakuja',
    required: 'Inahitajika',
    fldInboxName: 'Jina la sanduku',
  },

  // ---------- Input Items (Phase 4) ----------
  inputItem: {
    title: 'Pembejeo',
    type: 'Aina',
    typeFertilizer: 'Mbolea',
    typePesticide: 'Dawa za wadudu',
    typeSeed: 'Mbegu',
    typeConstruction: 'Ujenzi',
    typeSpareParts: 'Vipuri',
    typeTools: 'Zana',
    typePackaging: 'Ufungaji',
    typeOther: 'Nyingine',
    spec: 'Maelezo',
    activeIngredient: 'Kiungo amili',
    regNo: 'Nambari ya usajili',
    phi: 'PHI',
    defaultSupplier: 'Muuzaji chaguo-msingi',
  },

  // ---------- Warehouses (Phase 4 purpose) ----------
  wh: {
    purpose: 'Madhumuni',
    purposeFinishedGoods: 'Bidhaa zilizokamilika',
    purposeSeedStorage: 'Hifadhi ya mbegu',
    purposeFertilizerStorage: 'Hifadhi ya mbolea',
    purposePesticideStorage: 'Kabati la dawa',
    purposeConstructionStorage: 'Yadi ya ujenzi',
    purposeSparePartsStorage: 'Sehemu za vipuri',
    purposeToolsStorage: 'Chumba cha zana',
    purposePackagingStorage: 'Vifaa vya ufungaji',
    purposeOtherStorage: 'Hifadhi nyingine',
    level: 'Kiwango',
    selectLevel: 'Chagua kiwango',
    levelWarehouse: 'Ghala',
    levelZone: 'Eneo',
    levelShelf: 'Rafu',
    levelBin: 'Sehemu',
    expandAll: 'Panua zote',
    collapseAll: 'Kunja zote',
    confirmDelete: 'Futa ghala "{code} - {name}"? Haitaweza kurudishwa.',
  },

  // ---------- Mobile (PWA) - the worker's daily UI ----------
  m: {
    // shell
    home: 'Nyumbani',
    activity: 'Shughuli',
    harvest: 'Mavuno',
    tasks: 'Kazi',
    me: 'Mimi',
    offline: 'Nje ya mtandao',
    desktop: 'Kompyuta',
    syncPending: '{n} zinasubiri kusawazishwa',
    logoutConfirm: 'Una uhakika unataka kutoka?',

    // greetings
    greetMorning: 'Habari za asubuhi',
    greetAfternoon: 'Habari za mchana',
    greetEvening: 'Habari za jioni',
    greetNight: 'Usiku mwema',
    greetEarlyMorning: 'Umeamka mapema',

    // home
    myTasks: 'Kazi zangu',
    todayList: 'Kazi za leo',
    noTasks: 'Hakuna kazi - umefanya vyema!',
    viewAll: 'Tazama zote',
    sevHigh: 'Juu',
    sevMedium: 'Wastani',
    sevLow: 'Chini',

    // record forms
    recordActivity: 'Rekodi shughuli',
    recordHarvest: 'Rekodi mavuno',
    plan: 'Mpango wa kupanda',
    pickPlan: 'Chagua mpango',
    noPlans: 'Hakuna mpango unaoendelea - tengeneza kwenye kompyuta kwanza',
    activityType: 'Aina ya shughuli',
    date: 'Tarehe',
    harvestDate: 'Tarehe ya mavuno',
    qtyKg: 'Kiasi (kg)',
    qtyPlaceholder: 'mfano 125.5',
    photos: 'Picha',
    addPhoto: 'Ongeza picha',
    remarkPlaceholder: 'Maelezo (hali ya hewa, uchunguzi, matatizo...)',
    submitOk: 'Imewasilishwa',
    submitFail: 'Kuwasilisha kumeshindwa - imehifadhiwa, itajaribiwa tena ikipata mtandao',
    savedLocally: 'Imehifadhiwa hapa - itasawazishwa ikipata mtandao',
    syncedN: '{n} zimesawazishwa',

    // tasks
    tabToday: 'Leo',
    tabRisk: 'Hatari',
    tabFollow: 'Fuatilia',
    markDone: 'Imekamilika',
    dismiss: 'Puuza',
    markedDone: 'Imewekwa kuwa imekamilika',
    dismissed: 'Imepuuzwa',
    dueToday: 'Inahitaji leo',
    dueTomorrow: 'Inahitaji kesho',
    overdueDays: 'Imechelewa siku {n}',
    inDays: 'Baada ya siku {n}',

    // GPS
    gps: 'GPS',
    gpsPlaceholder: 'Bofya kupata mahali',
    locate: 'Pata mahali',
    gpsSuccess: 'Mahali pamepatikana (usahihi wa mita {m})',
    gpsAccuracy: 'Usahihi: mita {m}',
    gpsFailed: 'Kupata mahali kumeshindwa',
    gpsUnsupported: 'Kifaa hiki hakitumii GPS',
  },
}
