import React, { useState } from 'react';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer, Area, AreaChart 
} from 'recharts';
import { 
  TrendingUp, Bell, Users, Calendar, Mic, Search, ChevronDown, 
  MapPin, Plus, AlertCircle, Volume2, Leaf, ArrowUpRight, ArrowDownRight
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

// --- Mock Data ---
const historicalData = [
  { date: '2/26', price: 18.5 },
  { date: '2/27', price: 19.2 },
  { date: '2/28', price: 21.0 },
  { date: '3/01', price: 25.5 },
  { date: '3/02', price: 28.0 },
  { date: '3/03', price: 26.5 },
  { date: '3/04', price: 25.0 },
];

const communityReports = [
  { id: 1, market: '板橋湳興市場', price: 30, time: '10分鐘前', user: '王媽媽', type: '零售' },
  { id: 2, market: '台北一市', price: 25, time: '1小時前', user: '官方批發', type: '批發' },
  { id: 3, market: '三重果菜市場', price: 28, time: '2小時前', user: '陳先生', type: '零售' },
];

const seasonalProduce = [
  { name: '高麗菜', season: '冬季盛產', tip: '挑選葉片緊密、拿起來有重量感的。', icon: '🥬' },
  { name: '白蘿蔔', season: '冬季盛產', tip: '表皮光滑、輕敲有清脆聲。', icon: '🥕' },
  { name: '牛番茄', season: '初春盛產', tip: '顏色鮮紅均勻、蒂頭翠綠。', icon: '🍅' },
];

export default function App() {
  const [isElderlyMode, setIsElderlyMode] = useState(false);
  const [alertPrice, setAlertPrice] = useState('30');
  const [isSpeaking, setIsSpeaking] = useState(false);

  // Toggle classes based on mode
  const textBase = isElderlyMode ? 'text-xl font-medium' : 'text-sm';
  const textLg = isElderlyMode ? 'text-3xl font-bold' : 'text-lg font-semibold';
  const textXl = isElderlyMode ? 'text-5xl font-black' : 'text-3xl font-bold';
  const textSm = isElderlyMode ? 'text-lg' : 'text-xs';
  const pBase = isElderlyMode ? 'p-6' : 'p-4';
  const gapBase = isElderlyMode ? 'gap-6' : 'gap-4';

  const handleVoiceQuery = () => {
    setIsSpeaking(true);
    setTimeout(() => setIsSpeaking(false), 2000);
  };

  return (
    <div className={`min-h-screen transition-colors duration-300 ${isElderlyMode ? 'bg-white text-black' : 'bg-slate-50 text-slate-900'}`}>
      
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
            {isElderlyMode ? '切換一般模式' : '👵 長輩大字模式'}
          </button>
        </div>
      </header>

      <main className={`max-w-5xl mx-auto px-4 py-8 flex flex-col ${gapBase}`}>
        
        {/* Search & Voice (Elderly Friendly) */}
        <div className="flex gap-2">
          <div className={`flex-1 flex items-center rounded-2xl border-2 overflow-hidden ${isElderlyMode ? 'border-black bg-white' : 'border-slate-200 bg-white focus-within:border-emerald-500 focus-within:ring-4 focus-within:ring-emerald-500/20'}`}>
            <div className="pl-4 text-slate-400">
              <Search className={isElderlyMode ? 'w-8 h-8 text-black' : 'w-5 h-5'} />
            </div>
            <input 
              type="text" 
              placeholder="搜尋農產品 (例如：高麗菜)" 
              className={`w-full bg-transparent outline-none ${isElderlyMode ? 'p-4 text-2xl font-bold placeholder:text-slate-400' : 'p-3 text-sm'}`}
            />
          </div>
          <button 
            onClick={handleVoiceQuery}
            className={`flex items-center justify-center rounded-2xl transition-all ${
              isElderlyMode 
                ? 'w-20 bg-blue-600 text-white border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:translate-y-1 active:shadow-none' 
                : 'w-12 bg-emerald-100 text-emerald-600 hover:bg-emerald-200'
            }`}
          >
            {isSpeaking ? (
              <Volume2 className={`animate-pulse ${isElderlyMode ? 'w-10 h-10' : 'w-6 h-6'}`} />
            ) : (
              <Mic className={isElderlyMode ? 'w-10 h-10' : 'w-6 h-6'} />
            )}
          </button>
        </div>

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
                      台北一市
                    </span>
                    <span className={`px-2 py-1 rounded-md font-bold ${isElderlyMode ? 'bg-green-600 text-white text-lg' : 'bg-emerald-100 text-emerald-700 text-xs'}`}>
                      初秋高麗菜
                    </span>
                  </div>
                  <h2 className={textXl}>$25.0 <span className={textSm + ' text-slate-500 font-normal'}>/ 台斤</span></h2>
                </div>
                <div className={`flex items-center gap-1 font-bold ${isElderlyMode ? 'text-red-600 text-2xl bg-red-100 px-3 py-2 rounded-xl border-2 border-red-600' : 'text-red-500 text-sm bg-red-50 px-2 py-1 rounded-lg'}`}>
                  <ArrowUpRight className={isElderlyMode ? 'w-8 h-8' : 'w-4 h-4'} />
                  +12.5%
                </div>
              </div>

              {/* Chart Section */}
              <div className={pBase}>
                <div className="flex justify-between items-center mb-4">
                  <h3 className={`${textLg} flex items-center gap-2`}>
                    <TrendingUp className={isElderlyMode ? 'w-8 h-8 text-blue-600' : 'w-5 h-5 text-emerald-500'} />
                    歷史價格走勢 (7天)
                  </h3>
                </div>
                <div className="h-64 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={historicalData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
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
                <button className={`flex items-center gap-1 px-3 py-1.5 rounded-full font-bold ${isElderlyMode ? 'bg-orange-500 text-white text-lg border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]' : 'bg-orange-100 text-orange-700 text-sm hover:bg-orange-200'}`}>
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
                          {report.user} • {report.time}
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
                <p className={`${textSm} mt-2 opacity-90`}>當價格跌破設定值時，發送推播通知。</p>
              </div>
              <div className={pBase}>
                <div className="mb-4">
                  <label className={`block mb-2 font-bold ${textBase}`}>目標價格 (元/台斤)</label>
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
                <button className={`w-full py-3 rounded-xl font-bold transition-all ${isElderlyMode ? 'bg-black text-white text-xl border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:translate-y-1 active:shadow-none' : 'bg-blue-600 text-white hover:bg-blue-700'}`}>
                  設定提醒
                </button>
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
                  <div key={idx} className={`m-2 p-4 rounded-2xl ${isElderlyMode ? 'bg-emerald-50 border-2 border-black' : 'bg-slate-50 border border-slate-100'}`}>
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
                  </div>
                ))}
              </div>
            </div>

          </div>
        </div>
      </main>
    </div>
  );
}
