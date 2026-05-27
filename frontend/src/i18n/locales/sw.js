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
