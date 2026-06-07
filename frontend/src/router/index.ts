import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    component: () => import('@/layouts/BlankLayout.vue'),
    children: [
      {
        path: '',
        component: () => import('@/views/login/LoginView.vue'),
        meta: { public: true },
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue') },
      { path: 'merchant', component: () => import('@/views/merchant/MerchantList.vue') },
      { path: 'merchant/:id', component: () => import('@/views/merchant/MerchantDetail.vue') },
      { path: 'address', component: () => import('@/views/address/AddressList.vue') },
      { path: 'deposit', component: () => import('@/views/transaction/DepositList.vue') },
      { path: 'withdraw', component: () => import('@/views/transaction/WithdrawList.vue') },
      { path: 'wallet', component: () => import('@/views/wallet/WalletBalance.vue') },
      { path: 'sweep', component: () => import('@/views/sweep/SweepBatchList.vue') },
      { path: 'sweep/settings', component: () => import('@/views/sweep/SweepSettings.vue') },
      { path: 'sweep/:batchNo', component: () => import('@/views/sweep/SweepBatchDetail.vue') },
      { path: 'webhook', component: () => import('@/views/webhook/WebhookList.vue') },
      { path: 'webhook/log', component: () => import('@/views/webhook/WebhookLog.vue') },
      { path: 'risk', component: () => import('@/views/risk/RiskRules.vue') },
      { path: 'report', component: () => import('@/views/report/ReportExport.vue') },
      { path: 'chain-nodes', component: () => import('@/views/settings/ChainNodeSettings.vue') },
      { path: 'merchant-api-docs', component: () => import('@/views/docs/MerchantApiDocs.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return '/login'
  }
})

export default router
