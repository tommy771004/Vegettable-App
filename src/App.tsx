import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer, Area, AreaChart
} from 'recharts';
import {
  TrendingUp, Bell, Users, Calendar, Mic, Search, ChevronDown,
  MapPin, Plus, AlertCircle, Volume2, Leaf, ArrowUpRight, ArrowDownRight,
  X, Check, Star, CloudRain, ShoppingCart
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

// --- Types ---
interface ProduceItem {
  id: string;
  name: string;
  market: string;
  price: number;
  previousPrice: number;
  unit: string;
  category: string;
}

interface CommunityReport {
  id: number;
  market: string;
  price: number;
  time: string;
  user: string;
  type: '零售' | '批發';
  cropName: string;
}

interface PriceAlert {
  id: number;
  cropName: string;
  targetPrice: number;
  currentPrice: number;
  isTriggered: boolean;
}

interface SeasonalItem {
  name: string;
  season: string;
  tip: string;
  icon: string;
}

// --- Mock Data (structured for future API replacement) ---
const ALL_PRODUCE: ProduceItem[] = [
  { id: 'cabbage-001', name: '初秋高麗菜', market: '台北一市', price: 25.0, previousPrice: 22.2, unit: '台斤', category: '葉菜類' },
  { id: 'radish-001', name: '白蘿蔔', market: '台北一市', price: 12.5, previousPrice: 14.0, unit: '台斤', category: '根莖類' },
  { id: 'tomato-001', name: '牛番茄', market: '台北一市', price: 35.0, previousPrice: 32.0, unit: '台斤', category: '果菜類' },
  { id: 'spinach-001', name: '菠菜', market: '台北二市', price: 40.0, previousPrice: 38.5, unit: '台斤', category: '葉菜類' },
  { id: 'cucumber-001', name: '小黃瓜', market: '三重果菜市場', price: 22.0, previousPrice: 25.0, unit: '台斤', category: '果菜類' },
  { id: 'greenonion-001', name: '青蔥', market: '板橋果菜市場', price: 55.0, previousPrice: 48.0, unit: '台斤', category: '辛香類' },
  { id: 'sweetpotato-001', name: '地瓜葉', market: '台北一市', price: 18.0, previousPrice: 20.0, unit: '台斤', category: '葉菜類' },
  { id: 'chinesecabbage-001', name: '大白菜', market: '台北二市', price: 15.0, previousPrice: 16.5, unit: '台斤', category: '葉菜類' },
  { id: 'carrot-001', name: '紅蘿蔔', market: '三重果菜市場', price: 20.0, previousPrice: 19.0, unit: '台斤', category: '根莖類' },
  { id: 'mushroom-001', name: '香菇', market: '台北一市', price: 85.0, previousPrice: 80.0, unit: '台斤', category: '菇類' },
];

const PRICE_HISTORY: Record<string, { date: string; price: number }[]> = {
  'cabbage-001': [
    { date: '2/26', price: 18.5 }, { date: '2/27', price: 19.2 }, { date: '2/28', price: 21.0 },
    { date: '3/01', price: 25.5 }, { date: '3/02', price: 28.0 }, { date: '3/03', price: 26.5 }, { date: '3/04', price: 25.0 },
  ],
  'radish-001': [
    { date: '2/26', price: 15.0 }, { date: '2/27', price: 14.8 }, { date: '2/28', price: 14.0 },
    { date: '3/01', price: 13.5 }, { date: '3/02', price: 13.0 }, { date: '3/03', price: 12.8 }, { date: '3/04', price: 12.5 },
  ],
  'tomato-001': [
    { date: '2/26', price: 30.0 }, { date: '2/27', price: 31.0 }, { date: '2/28', price: 32.5 },
    { date: '3/01', price: 33.0 }, { date: '3/02', price: 34.0 }, { date: '3/03', price: 34.5 }, { date: '3/04', price: 35.0 },
  ],
};

const INITIAL_COMMUNITY_REPORTS: CommunityReport[] = [
  { id: 1, market: '板橋湳興市場', price: 30, time: '10分鐘前', user: '王媽媽', type: '零售', cropName: '高麗菜' },
  { id: 2, market: '台北一市', price: 25, time: '1小時前', user: '官方批發', type: '批發', cropName: '高麗菜' },
  { id: 3, market: '三重果菜市場', price: 28, time: '2小時前', user: '陳先生', type: '零售', cropName: '高麗菜' },
];

const seasonalProduce: SeasonalItem[] = [
  { name: '高麗菜', season: '冬季盛產', tip: '挑選葉片緊密、拿起來有重量感的。', icon: '🥬' },
  { name: '白蘿蔔', season: '冬季盛產', tip: '表皮光滑、輕敲有清脆聲。', icon: '🥕' },
  { name: '牛番茄', season: '初春盛產', tip: '顏色鮮紅均勻、蒂頭翠綠。', icon: '🍅' },
];

// --- Helper: calculate price change percentage ---
function calcChange(current: number, previous: number): number {
  if (previous === 0) return 0;
  return ((current - previous) / previous) * 100;
}

// --- Report Price Modal ---
function ReportPriceModal({ isOpen, onClose, onSubmit, isElderlyMode }: {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (report: { market: string; price: number; cropName: string; type: '零售' | '批發' }) => void;
  isElderlyMode: boolean;
}) {
  const [market, setMarket] = useState('');
  const [price, setPrice] = useState('');
  const [cropName, setCropName] = useState('');
  const [type, setType] = useState<'零售' | '批發'>('零售');

  const textBase = isElderlyMode ? 'text-xl font-medium' : 'text-sm';

  const handleSubmit = () => {
    if (!market || !price || !cropName) return;
    onSubmit({ market, price: parseFloat(price), cropName, type });
    setMarket('');
    setPrice('');
    setCropName('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={onClose}>
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.9 }}
        className={`bg-white rounded-3xl w-full max-w-md mx-4 overflow-hidden ${isElderlyMode ? 'border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]' : 'shadow-xl'}`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className={`flex items-center justify-between ${isElderlyMode ? 'bg-orange-500 text-white p-6 border-b-4 border-black' : 'bg-orange-50 text-orange-900 p-4 border-b border-orange-100'}`}>
          <h3 className={`${isElderlyMode ? 'text-2xl font-black' : 'text-lg font-semibold'} flex items-center gap-2`}>
            <MapPin className={isElderlyMode ? 'w-8 h-8' : 'w-5 h-5'} />
            回報在地價格
          </h3>
          <button onClick={onClose} className="p-1">
            <X className={isElderlyMode ? 'w-8 h-8' : 'w-5 h-5'} />
          </button>
        </div>
        <div className={isElderlyMode ? 'p-6 space-y-4' : 'p-4 space-y-3'}>
          <div>
            <label className={`block mb-1 font-bold ${textBase}`}>農產品名稱</label>
            <input
              type="text"
              placeholder="例如：高麗菜"
              value={cropName}
              onChange={(e) => setCropName(e.target.value)}
              className={`w-full rounded-xl border-2 outline-none ${isElderlyMode ? 'border-black p-4 text-2xl' : 'border-slate-200 p-3 text-sm focus:border-orange-500'}`}
            />
          </div>
          <div>
            <label className={`block mb-1 font-bold ${textBase}`}>市場名稱</label>
            <input
              type="text"
              placeholder="例如：板橋湳興市場"
              value={market}
              onChange={(e) => setMarket(e.target.value)}
              className={`w-full rounded-xl border-2 outline-none ${isElderlyMode ? 'border-black p-4 text-2xl' : 'border-slate-200 p-3 text-sm focus:border-orange-500'}`}
            />
          </div>
          <div>
            <label className={`block mb-1 font-bold ${textBase}`}>價格 (元/台斤)</label>
            <input
              type="number"
              placeholder="30"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className={`w-full rounded-xl border-2 outline-none ${isElderlyMode ? 'border-black p-4 text-2xl' : 'border-slate-200 p-3 text-sm focus:border-orange-500'}`}
            />
          </div>
          <div>
            <label className={`block mb-1 font-bold ${textBase}`}>類型</label>
            <div className="flex gap-2">
              {(['零售', '批發'] as const).map((t) => (
                <button
                  key={t}
                  onClick={() => setType(t)}
                  className={`flex-1 py-2 rounded-xl font-bold transition-all ${
                    type === t
                      ? (isElderlyMode ? 'bg-black text-white border-2 border-black' : 'bg-orange-500 text-white')
                      : (isElderlyMode ? 'bg-slate-100 border-2 border-black' : 'bg-slate-100 text-slate-600')
                  } ${isElderlyMode ? 'text-xl p-4' : 'text-sm'}`}
                >
                  {t}
                </button>
              ))}
            </div>
          </div>
          <button
            onClick={handleSubmit}
            disabled={!market || !price || !cropName}
            className={`w-full py-3 rounded-xl font-bold transition-all disabled:opacity-40 ${
              isElderlyMode
                ? 'bg-orange-500 text-white text-xl border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:translate-y-1 active:shadow-none'
                : 'bg-orange-500 text-white hover:bg-orange-600'
            }`}
          >
            送出回報
          </button>
        </div>
      </motion.div>
    </div>
  );
}

// --- Toast Notification ---
function Toast({ message, isVisible, type = 'success' }: { message: string; isVisible: boolean; type?: 'success' | 'info' | 'warning' }) {
  if (!isVisible) return null;

  const bgColor = type === 'success' ? 'bg-emerald-500' : type === 'warning' ? 'bg-amber-500' : 'bg-blue-500';

  return (
    <motion.div
      initial={{ opacity: 0, y: -50 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -50 }}
      className={`fixed top-4 left-1/2 -translate-x-1/2 z-[100] ${bgColor} text-white px-6 py-3 rounded-2xl shadow-lg font-bold text-sm flex items-center gap-2`}
    >
      {type === 'success' && <Check className="w-5 h-5" />}
      {type === 'warning' && <AlertCircle className="w-5 h-5" />}
      {type === 'info' && <Bell className="w-5 h-5" />}
      {message}
    </motion.div>
  );
}

// --- Main App ---
export default function App() {
  const [isElderlyMode, setIsElderlyMode] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedProduce, setSelectedProduce] = useState<ProduceItem>(ALL_PRODUCE[0]);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [isListening, setIsListening] = useState(false);

  // Price alert state
  const [alertPrice, setAlertPrice] = useState('30');
  const [alerts, setAlerts] = useState<PriceAlert[]>([]);

  // Community reports state
  const [communityReports, setCommunityReports] = useState<CommunityReport[]>(INITIAL_COMMUNITY_REPORTS);
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);

  // Toast state
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'info' | 'warning' } | null>(null);

  // Search results
  const [showSearchResults, setShowSearchResults] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  const filteredProduce = searchQuery.trim()
    ? ALL_PRODUCE.filter(p =>
        p.name.includes(searchQuery) || p.market.includes(searchQuery) || p.category.includes(searchQuery)
      )
    : [];

  // Close search dropdown when clicking outside
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setShowSearchResults(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Check alerts whenever selectedProduce changes
  useEffect(() => {
    setAlerts(prev =>
      prev.map(a =>
        a.cropName === selectedProduce.name
          ? { ...a, currentPrice: selectedProduce.price, isTriggered: selectedProduce.price <= a.targetPrice }
          : a
      )
    );
  }, [selectedProduce]);

  // Show toast helper
  const showToast = useCallback((message: string, type: 'success' | 'info' | 'warning' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  }, []);

  // --- Voice query using Web Speech API ---
  const handleVoiceQuery = useCallback(() => {
    const SpeechRecognitionAPI = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognitionAPI) {
      showToast('您的瀏覽器不支援語音辨識功能', 'warning');
      return;
    }

    const recognition = new SpeechRecognitionAPI();
    recognition.lang = 'zh-TW';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    setIsListening(true);
    setIsSpeaking(true);

    recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      setSearchQuery(transcript);
      setShowSearchResults(true);
      setIsListening(false);
      setIsSpeaking(false);
      showToast(`語音辨識結果：${transcript}`, 'info');
    };

    recognition.onerror = () => {
      setIsListening(false);
      setIsSpeaking(false);
      showToast('語音辨識失敗，請再試一次', 'warning');
    };

    recognition.onend = () => {
      setIsListening(false);
      setIsSpeaking(false);
    };

    recognition.start();
  }, [showToast]);

  // --- Text-to-speech for elderly mode ---
  const speakText = useCallback((text: string) => {
    if (!('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'zh-TW';
    utterance.rate = 0.85;
    window.speechSynthesis.speak(utterance);
  }, []);

  // --- Set price alert ---
  const handleSetAlert = useCallback(() => {
    const target = parseFloat(alertPrice);
    if (isNaN(target) || target <= 0) {
      showToast('請輸入有效的價格', 'warning');
      return;
    }

    const existingIdx = alerts.findIndex(a => a.cropName === selectedProduce.name);
    if (existingIdx >= 0) {
      setAlerts(prev => prev.map((a, i) =>
        i === existingIdx
          ? { ...a, targetPrice: target, isTriggered: selectedProduce.price <= target }
          : a
      ));
    } else {
      setAlerts(prev => [
        ...prev,
        {
          id: Date.now(),
          cropName: selectedProduce.name,
          targetPrice: target,
          currentPrice: selectedProduce.price,
          isTriggered: selectedProduce.price <= target,
        },
      ]);
    }

    showToast(`已設定「${selectedProduce.name}」跌至 $${target} 時通知您`, 'success');

    // Request browser notification permission
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }

    // If already triggered, notify immediately
    if (selectedProduce.price <= target) {
      showToast(`「${selectedProduce.name}」目前價格 $${selectedProduce.price} 已低於目標 $${target}！`, 'warning');
      if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('到價提醒', {
          body: `「${selectedProduce.name}」目前價格 $${selectedProduce.price} 已低於您設定的 $${target}`,
          icon: '🥬',
        });
      }
    }
  }, [alertPrice, alerts, selectedProduce, showToast]);

  // --- Submit community report ---
  const handleReportSubmit = useCallback((report: { market: string; price: number; cropName: string; type: '零售' | '批發' }) => {
    const newReport: CommunityReport = {
      id: Date.now(),
      market: report.market,
      price: report.price,
      time: '剛剛',
      user: '匿名用戶',
      type: report.type,
      cropName: report.cropName,
    };
    setCommunityReports(prev => [newReport, ...prev]);
    showToast('感謝您的回報！已獲得 +5 貢獻積分', 'success');
  }, [showToast]);

  // --- Select produce from search ---
  const handleSelectProduce = useCallback((item: ProduceItem) => {
    setSelectedProduce(item);
    setSearchQuery('');
    setShowSearchResults(false);
    if (isElderlyMode) {
      speakText(`${item.name}，目前在${item.market}的批發價為每${item.unit}${item.price}元`);
    }
  }, [isElderlyMode, speakText]);

  // Derived values
  const priceChange = calcChange(selectedProduce.price, selectedProduce.previousPrice);
  const isUp = priceChange > 0;
  const historyData = PRICE_HISTORY[selectedProduce.id] || PRICE_HISTORY['cabbage-001'];

  // Toggle classes based on mode
  const textBase = isElderlyMode ? 'text-xl font-medium' : 'text-sm';
  const textLg = isElderlyMode ? 'text-3xl font-bold' : 'text-lg font-semibold';
  const textXl = isElderlyMode ? 'text-5xl font-black' : 'text-3xl font-bold';
  const textSm = isElderlyMode ? 'text-lg' : 'text-xs';
  const pBase = isElderlyMode ? 'p-6' : 'p-4';
  const gapBase = isElderlyMode ? 'gap-6' : 'gap-4';

  return (
    <div className={`min-h-screen transition-colors duration-300 ${isElderlyMode ? 'bg-white text-black' : 'bg-slate-50 text-slate-900'}`}>

      {/* Toast */}
      <AnimatePresence>
        {toast && <Toast message={toast.message} isVisible={true} type={toast.type} />}
      </AnimatePresence>

      {/* Report Price Modal */}
      <AnimatePresence>
        <ReportPriceModal
          isOpen={isReportModalOpen}
          onClose={() => setIsReportModalOpen(false)}
          onSubmit={handleReportSubmit}
          isElderlyMode={isElderlyMode}
        />
      </AnimatePresence>

      {/* Header */}
      <header className={`sticky top-0 z-50 border-b ${isElderlyMode ? 'bg-yellow-400 border-black border-b-4' : 'bg-white border-slate-200 shadow-sm'}`}>
        <div className="max-w-5xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className={`flex items-center justify-center rounded-xl ${isElderlyMode ? 'bg-black text-yellow-400 w-12 h-12' : 'bg-emerald-500 text-white w-10 h-10'}`}>
              <Leaf className={isElderlyMode ? 'w-8 h-8' : 'w-6 h-6'} />
            </div>
            <h1 className={isElderlyMode ? 'text-2xl font-black tracking-tight' : 'text-xl font-bold tracking-tight text-slate-800'}>
              台灣農產即時通
            </h1>
          </div>

          <button
            onClick={() => setIsElderlyMode(!isElderlyMode)}
            className={`flex items-center gap-2 px-4 py-2 rounded-full font-bold transition-all ${
              isElderlyMode
                ? 'bg-black text-white text-lg border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:translate-y-1 active:shadow-none'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200 text-sm'
            }`}
          >
            {isElderlyMode ? '切換一般模式' : '長輩大字模式'}
          </button>
        </div>
      </header>

      <main className={`max-w-5xl mx-auto px-4 py-8 flex flex-col ${gapBase}`}>

        {/* Search & Voice */}
        <div className="flex gap-2 relative" ref={searchRef}>
          <div className={`flex-1 flex items-center rounded-2xl border-2 overflow-hidden ${isElderlyMode ? 'border-black bg-white' : 'border-slate-200 bg-white focus-within:border-emerald-500 focus-within:ring-4 focus-within:ring-emerald-500/20'}`}>
            <div className="pl-4 text-slate-400">
              <Search className={isElderlyMode ? 'w-8 h-8 text-black' : 'w-5 h-5'} />
            </div>
            <input
              type="text"
              placeholder="搜尋農產品 (例如：高麗菜)"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setShowSearchResults(e.target.value.trim().length > 0);
              }}
              onFocus={() => {
                if (searchQuery.trim().length > 0) setShowSearchResults(true);
              }}
              className={`w-full bg-transparent outline-none ${isElderlyMode ? 'p-4 text-2xl font-bold placeholder:text-slate-400' : 'p-3 text-sm'}`}
            />
          </div>
          <button
            onClick={handleVoiceQuery}
            className={`flex items-center justify-center rounded-2xl transition-all ${
              isElderlyMode
                ? 'w-20 bg-blue-600 text-white border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:translate-y-1 active:shadow-none'
                : 'w-12 bg-emerald-100 text-emerald-600 hover:bg-emerald-200'
            } ${isListening ? 'animate-pulse' : ''}`}
          >
            {isSpeaking ? (
              <Volume2 className={`animate-pulse ${isElderlyMode ? 'w-10 h-10' : 'w-6 h-6'}`} />
            ) : (
              <Mic className={isElderlyMode ? 'w-10 h-10' : 'w-6 h-6'} />
            )}
          </button>

          {/* Search Results Dropdown */}
          {showSearchResults && filteredProduce.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              className={`absolute top-full left-0 right-12 mt-2 bg-white rounded-2xl overflow-hidden z-40 ${
                isElderlyMode ? 'border-4 border-black shadow-[6px_6px_0px_0px_rgba(0,0,0,1)]' : 'border border-slate-200 shadow-lg'
              }`}
            >
              {filteredProduce.map((item) => {
                const change = calcChange(item.price, item.previousPrice);
                return (
                  <button
                    key={item.id}
                    onClick={() => handleSelectProduce(item)}
                    className={`w-full text-left flex items-center justify-between hover:bg-slate-50 transition-colors ${isElderlyMode ? 'p-5 border-b-2 border-black last:border-b-0' : 'p-3 border-b border-slate-100 last:border-b-0'}`}
                  >
                    <div>
                      <div className={`font-bold ${isElderlyMode ? 'text-xl' : 'text-sm'}`}>{item.name}</div>
                      <div className={`text-slate-500 ${isElderlyMode ? 'text-lg' : 'text-xs'}`}>{item.market} · {item.category}</div>
                    </div>
                    <div className="text-right">
                      <div className={`font-bold ${isElderlyMode ? 'text-2xl' : 'text-base'}`}>${item.price}</div>
                      <div className={`font-bold flex items-center justify-end gap-0.5 ${change > 0 ? 'text-red-500' : 'text-emerald-500'} ${isElderlyMode ? 'text-lg' : 'text-xs'}`}>
                        {change > 0 ? <ArrowUpRight className="w-3 h-3" /> : <ArrowDownRight className="w-3 h-3" />}
                        {change > 0 ? '+' : ''}{change.toFixed(1)}%
                      </div>
                    </div>
                  </button>
                );
              })}
            </motion.div>
          )}

          {showSearchResults && searchQuery.trim() && filteredProduce.length === 0 && (
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              className={`absolute top-full left-0 right-12 mt-2 bg-white rounded-2xl z-40 ${
                isElderlyMode ? 'border-4 border-black p-6' : 'border border-slate-200 shadow-lg p-4'
              }`}
            >
              <p className={`text-center text-slate-500 ${isElderlyMode ? 'text-xl' : 'text-sm'}`}>
                找不到「{searchQuery}」相關的農產品
              </p>
            </motion.div>
          )}
        </div>

        {/* Active Alerts Banner */}
        {alerts.some(a => a.isTriggered) && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className={`rounded-2xl ${isElderlyMode ? 'bg-amber-100 border-4 border-amber-600 p-5' : 'bg-amber-50 border border-amber-200 p-3'}`}
          >
            <div className={`flex items-center gap-2 font-bold ${isElderlyMode ? 'text-xl text-amber-800' : 'text-sm text-amber-700'}`}>
              <Bell className={isElderlyMode ? 'w-7 h-7' : 'w-4 h-4'} />
              到價通知
            </div>
            {alerts.filter(a => a.isTriggered).map(a => (
              <div key={a.id} className={`mt-2 ${isElderlyMode ? 'text-lg' : 'text-sm'} text-amber-800`}>
                「{a.cropName}」目前 ${a.currentPrice}，已低於目標 ${a.targetPrice}
              </div>
            ))}
          </motion.div>
        )}

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

          {/* Left Column: Price & Chart */}
          <div className="lg:col-span-2 flex flex-col gap-6">

            {/* Price Card */}
            <div className={`rounded-3xl border ${isElderlyMode ? 'bg-white border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]' : 'bg-white border-slate-200 shadow-sm'}`}>
              <div className={`${pBase} border-b ${isElderlyMode ? 'border-black border-b-4' : 'border-slate-100'} flex justify-between items-start`}>
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`px-2 py-1 rounded-md font-bold ${isElderlyMode ? 'bg-black text-white text-lg' : 'bg-slate-100 text-slate-600 text-xs'}`}>
                      {selectedProduce.market}
                    </span>
                    <span className={`px-2 py-1 rounded-md font-bold ${isElderlyMode ? 'bg-green-600 text-white text-lg' : 'bg-emerald-100 text-emerald-700 text-xs'}`}>
                      {selectedProduce.name}
                    </span>
                  </div>
                  <h2 className={textXl}>
                    ${selectedProduce.price.toFixed(1)} <span className={textSm + ' text-slate-500 font-normal'}>/ {selectedProduce.unit}</span>
                  </h2>
                </div>
                <button
                  onClick={() => {
                    if (isElderlyMode) {
                      speakText(`${selectedProduce.name}目前每${selectedProduce.unit}${selectedProduce.price}元，${isUp ? '比昨天漲了' : '比昨天跌了'}${Math.abs(priceChange).toFixed(1)}%`);
                    }
                  }}
                  className={`flex items-center gap-1 font-bold ${
                    isUp
                      ? (isElderlyMode ? 'text-red-600 text-2xl bg-red-100 px-3 py-2 rounded-xl border-2 border-red-600' : 'text-red-500 text-sm bg-red-50 px-2 py-1 rounded-lg')
                      : (isElderlyMode ? 'text-emerald-600 text-2xl bg-emerald-100 px-3 py-2 rounded-xl border-2 border-emerald-600' : 'text-emerald-500 text-sm bg-emerald-50 px-2 py-1 rounded-lg')
                  }`}
                >
                  {isUp ? <ArrowUpRight className={isElderlyMode ? 'w-8 h-8' : 'w-4 h-4'} /> : <ArrowDownRight className={isElderlyMode ? 'w-8 h-8' : 'w-4 h-4'} />}
                  {isUp ? '+' : ''}{priceChange.toFixed(1)}%
                </button>
              </div>

              {/* Chart Section */}
              <div className={pBase}>
                <div className="flex justify-between items-center mb-4">
                  <h3 className={`${textLg} flex items-center gap-2`}>
                    <TrendingUp className={isElderlyMode ? 'w-8 h-8 text-blue-600' : 'w-5 h-5 text-emerald-500'} />
                    歷史價格走勢 (7天)
                  </h3>
                  {isElderlyMode && (
                    <button
                      onClick={() => speakText(`${selectedProduce.name}過去7天的價格走勢：最高${Math.max(...historyData.map(d => d.price))}元，最低${Math.min(...historyData.map(d => d.price))}元`)}
                      className="bg-blue-600 text-white px-3 py-2 rounded-xl border-2 border-black font-bold text-lg"
                    >
                      <Volume2 className="w-6 h-6" />
                    </button>
                  )}
                </div>
                <div className="h-64 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={historyData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                      <defs>
                        <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor={isElderlyMode ? '#2563eb' : '#10b981'} stopOpacity={0.3}/>
                          <stop offset="95%" stopColor={isElderlyMode ? '#2563eb' : '#10b981'} stopOpacity={0}/>
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={isElderlyMode ? '#cbd5e1' : '#f1f5f9'} />
                      <XAxis
                        dataKey="date"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fontSize: isElderlyMode ? 16 : 12, fill: isElderlyMode ? '#000' : '#64748b', fontWeight: isElderlyMode ? 'bold' : 'normal' }}
                        dy={10}
                      />
                      <YAxis
                        axisLine={false}
                        tickLine={false}
                        tick={{ fontSize: isElderlyMode ? 16 : 12, fill: isElderlyMode ? '#000' : '#64748b', fontWeight: isElderlyMode ? 'bold' : 'normal' }}
                      />
                      <RechartsTooltip
                        contentStyle={{
                          borderRadius: '12px',
                          border: isElderlyMode ? '4px solid #000' : 'none',
                          boxShadow: isElderlyMode ? '4px 4px 0px 0px rgba(0,0,0,1)' : '0 10px 15px -3px rgba(0,0,0,0.1)',
                          fontSize: isElderlyMode ? '20px' : '14px',
                          fontWeight: 'bold'
                        }}
                      />
                      <Area
                        type="monotone"
                        dataKey="price"
                        stroke={isElderlyMode ? '#2563eb' : '#10b981'}
                        strokeWidth={isElderlyMode ? 6 : 3}
                        fillOpacity={1}
                        fill="url(#colorPrice)"
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>

            {/* Community Reports */}
            <div className={`rounded-3xl border ${isElderlyMode ? 'bg-white border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]' : 'bg-white border-slate-200 shadow-sm'}`}>
              <div className={`${pBase} border-b ${isElderlyMode ? 'border-black border-b-4' : 'border-slate-100'} flex justify-between items-center`}>
                <h3 className={`${textLg} flex items-center gap-2`}>
                  <Users className={isElderlyMode ? 'w-8 h-8 text-orange-500' : 'w-5 h-5 text-orange-500'} />
                  在地回報 (批發 vs 零售)
                </h3>
                <button
                  onClick={() => setIsReportModalOpen(true)}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded-full font-bold ${isElderlyMode ? 'bg-orange-500 text-white text-lg border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]' : 'bg-orange-100 text-orange-700 text-sm hover:bg-orange-200'}`}
                >
                  <Plus className="w-4 h-4" /> 回報價格
                </button>
              </div>
              <div className="divide-y divide-slate-100">
                {communityReports.map((report) => (
                  <div key={report.id} className={`${pBase} flex items-center justify-between hover:bg-slate-50 transition-colors`}>
                    <div className="flex items-start gap-3">
                      <div className={`mt-1 flex items-center justify-center rounded-full ${isElderlyMode ? 'w-12 h-12 bg-slate-200 border-2 border-black' : 'w-8 h-8 bg-slate-100'}`}>
                        <MapPin className={isElderlyMode ? 'w-6 h-6 text-black' : 'w-4 h-4 text-slate-500'} />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className={`${textBase} font-bold`}>{report.market}</span>
                          <span className={`px-2 py-0.5 rounded text-xs font-bold ${report.type === '零售' ? 'bg-blue-100 text-blue-700' : 'bg-purple-100 text-purple-700'}`}>
                            {report.type}
                          </span>
                        </div>
                        <div className={`${textSm} text-slate-500 mt-1`}>
                          {report.user} · {report.time} · {report.cropName}
                        </div>
                      </div>
                    </div>
                    <div className={`${textLg} font-bold`}>
                      ${report.price}
                    </div>
                  </div>
                ))}
              </div>
            </div>

          </div>

          {/* Right Column: Alerts & Calendar */}
          <div className="flex flex-col gap-6">

            {/* Price Alert */}
            <div className={`rounded-3xl border overflow-hidden ${isElderlyMode ? 'bg-white border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]' : 'bg-white border-slate-200 shadow-sm'}`}>
              <div className={`${isElderlyMode ? 'bg-blue-600 text-white p-6 border-b-4 border-black' : 'bg-blue-50 text-blue-900 p-4 border-b border-blue-100'}`}>
                <h3 className={`${textLg} flex items-center gap-2`}>
                  <Bell className={isElderlyMode ? 'w-8 h-8' : 'w-5 h-5'} />
                  到價提醒
                </h3>
                <p className={`${textSm} mt-2 opacity-90`}>
                  當「{selectedProduce.name}」價格跌破設定值時通知您。
                </p>
              </div>
              <div className={pBase}>
                <div className="mb-4">
                  <label className={`block mb-2 font-bold ${textBase}`}>目標價格 (元/{selectedProduce.unit})</label>
                  <div className={`flex items-center rounded-xl border-2 overflow-hidden ${isElderlyMode ? 'border-black bg-slate-100' : 'border-slate-200 bg-white'}`}>
                    <span className={`pl-4 font-bold text-slate-400 ${textLg}`}>$</span>
                    <input
                      type="number"
                      value={alertPrice}
                      onChange={(e) => setAlertPrice(e.target.value)}
                      className={`w-full bg-transparent outline-none font-bold ${isElderlyMode ? 'p-4 text-3xl' : 'p-3 text-lg'}`}
                    />
                  </div>
                </div>
                <button
                  onClick={handleSetAlert}
                  className={`w-full py-3 rounded-xl font-bold transition-all ${isElderlyMode ? 'bg-black text-white text-xl border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:translate-y-1 active:shadow-none' : 'bg-blue-600 text-white hover:bg-blue-700'}`}
                >
                  設定提醒
                </button>

                {/* Show active alerts */}
                {alerts.length > 0 && (
                  <div className="mt-4 space-y-2">
                    <div className={`font-bold ${textSm} text-slate-500`}>已設定提醒：</div>
                    {alerts.map(a => (
                      <div
                        key={a.id}
                        className={`flex items-center justify-between rounded-lg ${
                          a.isTriggered
                            ? (isElderlyMode ? 'bg-amber-100 border-2 border-amber-500 p-3' : 'bg-amber-50 border border-amber-200 p-2')
                            : (isElderlyMode ? 'bg-slate-100 border-2 border-black p-3' : 'bg-slate-50 border border-slate-200 p-2')
                        }`}
                      >
                        <div>
                          <div className={`font-bold ${isElderlyMode ? 'text-lg' : 'text-xs'}`}>
                            {a.cropName} {a.isTriggered ? '(已觸發!)' : ''}
                          </div>
                          <div className={`text-slate-500 ${isElderlyMode ? 'text-base' : 'text-xs'}`}>
                            目標 ${a.targetPrice} / 目前 ${a.currentPrice}
                          </div>
                        </div>
                        <button
                          onClick={() => setAlerts(prev => prev.filter(x => x.id !== a.id))}
                          className="text-slate-400 hover:text-red-500"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Seasonal Calendar */}
            <div className={`rounded-3xl border ${isElderlyMode ? 'bg-white border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]' : 'bg-white border-slate-200 shadow-sm'}`}>
              <div className={`${pBase} border-b ${isElderlyMode ? 'border-black border-b-4' : 'border-slate-100'}`}>
                <h3 className={`${textLg} flex items-center gap-2`}>
                  <Calendar className={isElderlyMode ? 'w-8 h-8 text-emerald-600' : 'w-5 h-5 text-emerald-500'} />
                  當季蔬果曆
                </h3>
              </div>
              <div className="p-2">
                {seasonalProduce.map((item, idx) => (
                  <button
                    key={idx}
                    onClick={() => {
                      const found = ALL_PRODUCE.find(p => p.name.includes(item.name.slice(0, 2)));
                      if (found) handleSelectProduce(found);
                      if (isElderlyMode) {
                        speakText(`${item.name}，${item.season}。小秘訣：${item.tip}`);
                      }
                    }}
                    className={`w-full text-left m-2 p-4 rounded-2xl transition-colors ${
                      isElderlyMode ? 'bg-emerald-50 border-2 border-black hover:bg-emerald-100' : 'bg-slate-50 border border-slate-100 hover:bg-slate-100'
                    }`}
                  >
                    <div className="flex items-center gap-3 mb-2">
                      <span className="text-3xl">{item.icon}</span>
                      <div>
                        <h4 className={`${textBase} font-bold`}>{item.name}</h4>
                        <span className={`text-xs font-bold px-2 py-0.5 rounded ${isElderlyMode ? 'bg-emerald-200 text-emerald-900' : 'bg-emerald-100 text-emerald-700'}`}>
                          {item.season}
                        </span>
                      </div>
                    </div>
                    <p className={`${textSm} text-slate-600 mt-2 flex items-start gap-1`}>
                      <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                      {item.tip}
                    </p>
                  </button>
                ))}
              </div>
            </div>

          </div>
        </div>
      </main>
    </div>
  );
}
