import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/layouts/AppLayout.vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import PortalLayout from '@/layouts/PortalLayout.vue'
import i18n from '@/i18n'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, titleKey: 'auth.login' },
  },
  // 所有受保护路由都套在 AppLayout 下
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/Home.vue'),
        meta: { titleKey: 'menu.home' },
      },
      {
        path: 'master/crops',
        name: 'crops',
        component: () => import('@/views/master/CropList.vue'),
        meta: { titleKey: 'menu.crops' },
      },
      {
        path: 'master/varieties',
        name: 'varieties',
        component: () => import('@/views/master/VarietyList.vue'),
        meta: { titleKey: 'menu.varieties' },
      },
      {
        path: 'master/packaging-specs',
        name: 'packaging-specs',
        component: () => import('@/views/master/PackagingSpecList.vue'),
        meta: { titleKey: 'menu.packagingSpecs' },
      },
      {
        path: 'master/warehouses',
        name: 'warehouses',
        component: () => import('@/views/master/WarehouseList.vue'),
        meta: { titleKey: 'menu.warehouses' },
      },
      {
        path: 'master/input-items',
        name: 'input-items',
        component: () => import('@/views/master/InputItemList.vue'),
        meta: { titleKey: 'menu.inputItems' },
      },
      {
        path: 'master/input-stock',
        name: 'input-stock',
        component: () => import('@/views/master/InputStockList.vue'),
        meta: { titleKey: 'menu.inputStock' },
      },
      {
        path: 'master/stock-log',
        name: 'stock-log',
        component: () => import('@/views/master/InputStockLogList.vue'),
        meta: { titleKey: 'menu.stockLog' },
      },
      {
        path: 'warehouse/outbound',
        name: 'warehouse-outbound',
        component: () => import('@/views/warehouse/OutboundList.vue'),
        meta: { titleKey: 'menu.outbound' },
      },
      {
        path: 'qc/inspections',
        name: 'qc-inspections',
        component: () => import('@/views/qc/QcInspectionList.vue'),
        meta: { titleKey: 'menu.qcInspections' },
      },
      {
        path: 'qc/trace',
        name: 'qc-trace',
        component: () => import('@/views/qc/TracePage.vue'),
        meta: { titleKey: 'menu.qcTrace' },
      },
      {
        path: 'qc/complaints',
        name: 'qc-complaints',
        component: () => import('@/views/qc/ComplaintList.vue'),
        meta: { titleKey: 'menu.complaints' },
      },
      {
        path: 'qc/recalls',
        name: 'qc-recalls',
        component: () => import('@/views/qc/RecallList.vue'),
        meta: { titleKey: 'menu.recalls' },
      },
      {
        path: 'qc/gap-reports',
        name: 'qc-gap-reports',
        component: () => import('@/views/qc/GapReports.vue'),
        meta: { titleKey: 'menu.gapReports' },
      },
      {
        path: 'warehouse/reports',
        name: 'warehouse-reports',
        component: () => import('@/views/warehouse/WarehouseReports.vue'),
        meta: { titleKey: 'menu.warehouseReports' },
      },
      {
        path: 'warehouse/scrap',
        name: 'warehouse-scrap',
        component: () => import('@/views/warehouse/ScrapList.vue'),
        meta: { titleKey: 'menu.scrap' },
      },
      {
        path: 'warehouse/transfer',
        name: 'warehouse-transfer',
        component: () => import('@/views/warehouse/TransferList.vue'),
        meta: { titleKey: 'menu.transfer' },
      },
      {
        path: 'warehouse/stocktake',
        name: 'warehouse-stocktake',
        component: () => import('@/views/warehouse/StocktakeList.vue'),
        meta: { titleKey: 'menu.stocktake' },
      },
      {
        path: 'warehouse/inbound',
        name: 'warehouse-inbound',
        component: () => import('@/views/warehouse/InboundList.vue'),
        meta: { titleKey: 'menu.inbound' },
      },
      {
        path: 'production/plots',
        name: 'plots',
        component: () => import('@/views/production/PlotList.vue'),
        meta: { titleKey: 'menu.plots' },
      },
      {
        path: 'production/planting-plans',
        name: 'planting-plans',
        component: () => import('@/views/production/PlantingPlanList.vue'),
        meta: { titleKey: 'menu.plantingPlans' },
      },
      {
        path: 'production/activities',
        name: 'activities',
        component: () => import('@/views/production/ActivityList.vue'),
        meta: { titleKey: 'menu.activities' },
      },
      {
        path: 'production/harvests',
        name: 'harvests',
        component: () => import('@/views/production/HarvestList.vue'),
        meta: { titleKey: 'menu.harvests' },
      },
      {
        path: 'production/batches',
        name: 'batches',
        component: () => import('@/views/production/BatchList.vue'),
        meta: { titleKey: 'menu.batches' },
      },
      {
        path: 'production/batches/:id',
        name: 'batch-detail',
        component: () => import('@/views/production/BatchDetail.vue'),
        meta: { titleKey: 'batch.detailTitle' },
      },
      {
        path: 'packhouse/packings',
        name: 'packings',
        component: () => import('@/views/packhouse/PackingList.vue'),
        meta: { titleKey: 'menu.packings' },
      },
      {
        path: 'packhouse/inventory',
        name: 'inventory',
        component: () => import('@/views/packhouse/InventoryList.vue'),
        meta: { titleKey: 'menu.inventory' },
      },
      {
        path: 'sales/customers',
        name: 'customers',
        component: () => import('@/views/sales/CustomerList.vue'),
        meta: { titleKey: 'menu.customers' },
      },
      {
        path: 'sales/orders',
        name: 'orders',
        component: () => import('@/views/sales/OrderList.vue'),
        meta: { titleKey: 'menu.orders' },
      },
      {
        path: 'sales/orders/:id',
        name: 'order-detail',
        component: () => import('@/views/sales/OrderDetail.vue'),
        meta: { titleKey: 'order.detailTitle' },
      },
      {
        path: 'operations/action-board',
        name: 'action-board',
        component: () => import('@/views/operations/ActionBoard.vue'),
        meta: { titleKey: 'menu.actionBoard' },
      },
      {
        path: 'finance/reports',
        name: 'finance-reports',
        component: () => import('@/views/finance/FinanceReports.vue'),
        meta: { titleKey: 'menu.reports' },
      },
      {
        path: 'finance/ar',
        name: 'finance-ar',
        component: () => import('@/views/finance/AccountsReceivable.vue'),
        meta: { titleKey: 'menu.ar' },
      },
      {
        path: 'finance/monthly',
        name: 'finance-monthly',
        component: () => import('@/views/finance/MonthlyReport.vue'),
        meta: { titleKey: 'menu.monthly' },
      },
      {
        path: 'finance/cash-flow',
        name: 'finance-cash-flow',
        component: () => import('@/views/finance/CashFlowForecast.vue'),
        meta: { titleKey: 'menu.cashFlow' },
      },
      {
        path: 'procurement/suppliers',
        name: 'suppliers',
        component: () => import('@/views/procurement/SupplierList.vue'),
        meta: { titleKey: 'menu.suppliers' },
      },
      {
        path: 'procurement/orders',
        name: 'purchase-orders',
        component: () => import('@/views/procurement/PurchaseOrderList.vue'),
        meta: { titleKey: 'menu.purchaseOrders' },
      },
      {
        path: 'procurement/orders/:id',
        name: 'purchase-order-detail',
        component: () => import('@/views/procurement/PurchaseOrderDetail.vue'),
        meta: { titleKey: 'menu.purchaseOrders' },
      },
      {
        path: 'procurement/ap',
        name: 'accounts-payable',
        component: () => import('@/views/procurement/AccountsPayable.vue'),
        meta: { titleKey: 'menu.ap' },
      },
      {
        path: 'demo/files',
        name: 'file-demo',
        component: () => import('@/views/FileDemo.vue'),
        meta: { titleKey: 'menu.fileDemo' },
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('@/views/system/UserList.vue'),
        meta: { titleKey: 'menu.sysUsers' },
      },
      {
        path: 'system/roles',
        name: 'system-roles',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { titleKey: 'menu.sysRoles' },
      },
    ],
  },
  // ---------- Mobile (PWA) ----------
  {
    path: '/m',
    component: MobileLayout,
    meta: { mobile: true },
    children: [
      {
        path: '',
        name: 'm-home',
        component: () => import('@/views/mobile/MobileHome.vue'),
        meta: { titleKey: 'm.home', mobile: true },
      },
      {
        path: 'activity/new',
        name: 'm-activity-new',
        component: () => import('@/views/mobile/MobileActivityNew.vue'),
        meta: { titleKey: 'm.recordActivity', mobile: true },
      },
      {
        path: 'harvest/new',
        name: 'm-harvest-new',
        component: () => import('@/views/mobile/MobileHarvestNew.vue'),
        meta: { titleKey: 'm.recordHarvest', mobile: true },
      },
      {
        path: 'tasks',
        name: 'm-tasks',
        component: () => import('@/views/mobile/MobileTasks.vue'),
        meta: { titleKey: 'm.myTasks', mobile: true },
      },
      // 20.8: me - 占位
      {
        path: 'me',
        name: 'm-me',
        component: () => import('@/views/mobile/MobileHome.vue'),
        meta: { titleKey: 'm.me', mobile: true, todo: '20.8' },
      },
    ],
  },
  // ---------- Customer Portal (Sprint 37) ----------
  {
    path: '/portal',
    component: PortalLayout,
    meta: { portal: true },
    children: [
      { path: '', redirect: '/portal/orders' },
      {
        path: 'orders',
        name: 'portal-orders',
        component: () => import('@/views/portal/PortalOrders.vue'),
        meta: { titleKey: 'portal.myOrders', portal: true },
      },
    ],
  },
  // ---------- Public trace (QR scan landing, no auth, no layout) ----------
  {
    path: '/trace/:code',
    name: 'public-trace',
    component: () => import('@/views/qc/TracePage.vue'),
    props: route => ({ publicMode: true }),
    meta: { public: true, titleKey: 'trace.title' },
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const t = i18n.global.t
  const brand = t('brand')
  document.title = to.meta.titleKey ? `${t(to.meta.titleKey)} · ${brand}` : brand

  if (!to.meta.public && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isLoggedIn) {
    // Sprint 37: CUSTOMER -> /portal, WORKER -> /m, else use landingPath or /
    if (auth.isCustomer) return { path: '/portal/orders' }
    if (auth.isWorkerOnly) return { path: '/m/' }
    return { path: auth.landingPath || '/' }
  }
  // Sprint 20.8: WORKER role locked to /m  (public trace exempt)
  if (auth.isLoggedIn && auth.isWorkerOnly && !to.meta.mobile && !to.meta.public && to.name !== 'login') {
    return { path: '/m/' }
  }
  // Sprint 37: CUSTOMER role locked to /portal
  if (auth.isLoggedIn && auth.isCustomer && !to.meta.portal && !to.meta.public && to.name !== 'login') {
    return { path: '/portal/orders' }
  }
})

export default router
