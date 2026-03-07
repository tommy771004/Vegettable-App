// =============================================================================
// api.ts — React 前端 API 服務層
//
// 架構：
//   - 從 VITE_API_URL 讀取後端位址（可在 .env.local 設定）
//   - 每個函式回傳後端資料；若 API 未設定或失敗，呼叫端應降級到 mock 資料
//   - 使用 JWT Token（儲存在 sessionStorage，每次 App 啟動重新申請）
//
// 設定方式：
//   1. 複製 .env.example → .env.local
//   2. 設定 VITE_API_URL=https://your-backend-url
//   3. 重啟 vite dev server
// =============================================================================

const API_BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '');
const TOKEN_KEY = 'veg_jwt_token';
const DEVICE_KEY = 'veg_device_id';

// MARK: - 裝置 ID（持久化至 localStorage）
function getDeviceId(): string {
  let id = localStorage.getItem(DEVICE_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(DEVICE_KEY, id);
  }
  return id;
}

// MARK: - JWT 管理
async function ensureToken(): Promise<string | null> {
  if (!API_BASE) return null;
  const cached = sessionStorage.getItem(TOKEN_KEY);
  if (cached) return cached;
  try {
    const res = await fetch(`${API_BASE.replace('/api/produce', '')}/auth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId: getDeviceId() }),
    });
    if (!res.ok) return null;
    const data = await res.json();
    sessionStorage.setItem(TOKEN_KEY, data.token);
    return data.token as string;
  } catch {
    return null;
  }
}

// MARK: - 通用 fetch 封裝（自動附帶 JWT）
async function apiFetch<T>(path: string, opts?: RequestInit): Promise<T | null> {
  if (!API_BASE) return null;
  const token = await ensureToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(opts?.headers as Record<string, string>),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
  try {
    const res = await fetch(`${API_BASE}${path}`, { ...opts, headers });
    if (res.status === 401) {
      // Token 過期，清除後重試一次
      sessionStorage.removeItem(TOKEN_KEY);
      const newToken = await ensureToken();
      if (!newToken) return null;
      const retry = await fetch(`${API_BASE}${path}`, {
        ...opts,
        headers: { ...headers, Authorization: `Bearer ${newToken}` },
      });
      return retry.ok ? (retry.json() as Promise<T>) : null;
    }
    return res.ok ? (res.json() as Promise<T>) : null;
  } catch {
    return null;
  }
}

// =============================================================================
// MARK: - 型別定義
// =============================================================================

export interface ApiProduceItem {
  cropCode: string;
  cropName: string;
  marketCode: string;
  marketName: string;
  avgPrice: number;
  transQuantity: number;
  date: string;
}

export interface PaginatedResponse<T> {
  currentPage: number;
  totalPages: number;
  totalItems: number;
  data: T[];
}

export interface PriceAnomalyItem {
  cropCode: string;
  cropName: string;
  currentPrice: number;
  previousPrice: number;
  increasePercentage: number;
  alertMessage: string;
}

export interface SeasonalItem {
  cropCode: string;
  cropName: string;
  season: string;
  description: string;
}

export interface HistoricalPrice {
  date: string;
  avgPrice: number;
}

export interface WeatherAlert {
  alertType: 'None' | 'Typhoon' | 'HeavyRain';
  severity?: string;
  title?: string;
  message?: string;
  affectedCrops?: string[];
}

export interface BudgetRecipe {
  recipeName: string;
  mainIngredients: string[];
  reason: string;
  imageUrl: string;
  steps: string[];
}

export interface MarketComparison {
  marketName: string;
  avgPrice: number;
  transQuantity: number;
  date: string;
}

export interface CommunityReportPayload {
  cropCode: string;
  cropName: string;
  marketName: string;
  retailPrice: number;
  reportDate?: string;
}

// =============================================================================
// MARK: - API 函式
// =============================================================================

/** 今日批發價格列表（分頁 + 關鍵字） */
export async function fetchDailyPrices(
  keyword = '',
  page = 1,
  pageSize = 50
): Promise<PaginatedResponse<ApiProduceItem> | null> {
  const q = new URLSearchParams({ keyword, page: String(page), pageSize: String(pageSize) });
  return apiFetch<PaginatedResponse<ApiProduceItem>>(`/daily-prices?${q}`);
}

/** 價格異常警告（單日漲幅 >50%） */
export async function fetchAnomalies(): Promise<PriceAnomalyItem[] | null> {
  return apiFetch<PriceAnomalyItem[]>('/anomalies');
}

/** 當季農產品 */
export async function fetchSeasonal(): Promise<SeasonalItem[] | null> {
  return apiFetch<SeasonalItem[]>('/seasonal');
}

/** 指定農產品 30 日歷史價格 */
export async function fetchPriceHistory(cropCode: string): Promise<HistoricalPrice[] | null> {
  return apiFetch<HistoricalPrice[]>(`/history/${encodeURIComponent(cropCode)}`);
}

/** 天氣/颱風警報 */
export async function fetchWeatherAlert(): Promise<WeatherAlert | null> {
  return apiFetch<WeatherAlert>('/weather-alerts');
}

/** 省錢食譜推薦 */
export async function fetchBudgetRecipes(): Promise<BudgetRecipe[] | null> {
  return apiFetch<BudgetRecipe[]>('/budget-recipes');
}

/** 各市場比價（由低到高） */
export async function fetchMarketComparison(cropName: string): Promise<MarketComparison[] | null> {
  return apiFetch<MarketComparison[]>(`/compare/${encodeURIComponent(cropName)}`);
}

/** 回報社群物價 */
export async function submitCommunityReport(payload: CommunityReportPayload): Promise<boolean> {
  const result = await apiFetch<unknown>('/community-price', {
    method: 'POST',
    body: JSON.stringify({ ...payload, reportDate: payload.reportDate ?? new Date().toISOString() }),
  });
  return result !== null;
}

/** 是否已設定後端 URL */
export const isApiConfigured = (): boolean => Boolean(API_BASE);
