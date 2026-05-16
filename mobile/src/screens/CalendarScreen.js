import React, { useCallback, useState } from 'react';
import { View, Text, Pressable, RefreshControl } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, SectionTitle, Empty } from '../components/ui';
import { Api } from '../api/client';

const WEEKDAYS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

function pad(n) { return String(n).padStart(2, '0'); }

export default function CalendarScreen() {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth());
  const [data, setData] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const from = `${year}-${pad(month + 1)}-01`;
      const to = `${year}-${pad(month + 1)}-${pad(new Date(year, month + 1, 0).getDate())}`;
      const d = await Api.analytics(from, to);
      setData(d);
    } catch (e) {
      setData(null);
    }
  }, [year, month]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const busy = useCallback(
    (d) => {
      if (!data) return 0;
      const key = `${year}-${pad(month + 1)}-${pad(d)}`;
      let n = 0;
      if ((data.productivity?.series || []).some((s) => s.date === key && s.planned > 0)) n += 1;
      if ((data.study?.daily || []).some((s) => s.date === key && s.minutes > 0)) n += 1;
      if ((data.sleep?.series || []).some((s) => s.date === key)) n += 1;
      if ((data.activities?.timeline || []).some((t) => (t.start || '').slice(0, 10) === key)) n += 1;
      return n;
    },
    [data, year, month]
  );

  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstBlank = new Date(year, month, 1).getDay();
  const cells = [...Array(firstBlank).fill(null), ...Array.from({ length: daysInMonth }, (_, i) => i + 1)];
  const today = now.toISOString().slice(0, 10);
  const monthLabel = new Date(year, month, 1).toLocaleString('en-US', { month: 'long', year: 'numeric' });

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Row className="justify-between items-center mb-3">
        <Text style={tw`text-white font-extrabold text-2xl`}>Calendar</Text>
      </Row>
      <Card>
        <Row className="justify-between items-center mb-3">
          <Pressable onPress={() => setMonth((m) => { const nm = m - 1; if (nm < 0) { setYear(y => y - 1); return 11; } return nm; })} style={tw`bg-slate-800 rounded-lg px-3 py-1`}>
            <Text style={tw`text-white`}>‹</Text>
          </Pressable>
          <Text style={tw`text-white font-bold`}>{monthLabel}</Text>
          <Pressable onPress={() => setMonth((m) => { const nm = m + 1; if (nm > 11) { setYear(y => y + 1); return 0; } return nm; })} style={tw`bg-slate-800 rounded-lg px-3 py-1`}>
            <Text style={tw`text-white`}>›</Text>
          </Pressable>
        </Row>
        <View style={tw`flex-row mb-2`}>
          {WEEKDAYS.map((d, i) => <Text key={i} style={tw`flex-1 text-center text-slate-500 text-xs`}>{d}</Text>)}
        </View>
        <View style={tw`flex-row flex-wrap`}>
          {cells.map((d, i) => {
            const dateKey = d ? `${year}-${pad(month + 1)}-${pad(d)}` : null;
            const isToday = dateKey === today;
            const b = d ? busy(d) : 0;
            return (
              <View key={i} style={tw`w-[14.28%] aspect-square items-center justify-center p-0.5`}>
                {d ? (
                  <View style={tw`w-9 h-9 rounded-full items-center justify-center ${isToday ? 'bg-indigo-600' : 'bg-slate-800'}`}>
                    <Text style={tw`text-white text-xs ${isToday ? 'font-bold' : ''}`}>{d}</Text>
                  </View>
                ) : null}
                {b > 0 ? <View style={tw`w-1.5 h-1.5 rounded-full bg-emerald-400 mt-0.5 ${b > 1 ? 'bg-emerald-500' : ''}`} /> : null}
              </View>
            );
          })}
        </View>
      </Card>
      <Text style={tw`text-slate-500 text-xs mt-2 mb-3`}>Dots mark days you recorded tasks, study, sleep, or activities.</Text>

      <SectionTitle>This month</SectionTitle>
      {data ? (
        <>
          <Row className="gap-3 mb-3">
            <Card className="flex-1">
              <Text style={tw`text-[11px] text-slate-400 uppercase`}>Study</Text>
              <Text style={tw`text-xl font-bold text-white mt-1`}>{Math.round((data.study?.totalMinutes || 0) / 60)}h</Text>
            </Card>
            <Card className="flex-1">
              <Text style={tw`text-[11px] text-slate-400 uppercase`}>Tasks done</Text>
              <Text style={tw`text-xl font-bold text-white mt-1`}>{data.productivity?.totalCompleted ?? 0}</Text>
            </Card>
          </Row>
          <Card className="mb-3">
            <Text style={tw`text-xs text-slate-400 uppercase mb-1`}>Active days (13+ studied & tracked)</Text>
            <Text style={tw`text-white font-bold text-lg`}>{countActiveDays(data)} days</Text>
          </Card>
          <Row className="gap-3 mb-3">
            <Card className="flex-1">
              <Text style={tw`text-[11px] text-slate-400 uppercase`}>Sleep avg</Text>
              <Text style={tw`text-xl font-bold text-white mt-1`}>{data.sleep?.daysTracked ? Math.round((data.sleep?.avgDurationMinutes || 0) / 60) : '—'}h</Text>
            </Card>
            <Card className="flex-1">
              <Text style={tw`text-[11px] text-slate-400 uppercase`}>Habit rate</Text>
              <Text style={tw`text-xl font-bold text-white mt-1`}>{data.habits?.completionRate ?? '—'}%</Text>
            </Card>
          </Row>
        </>
      ) : (
        <Empty text="No data for this month." />
      )}
    </Screen>
  );
}

function countActiveDays(data) {
  const active = new Set();
  (data.study?.daily || []).forEach((s) => { if (s.minutes > 0) active.add(s.date); });
  (data.productivity?.series || []).forEach((s) => { if (s.completed > 0) active.add(s.date); });
  (data.sleep?.series || []).forEach((s) => active.add(s.date));
  (data.activities?.timeline || []).forEach((t) => active.add((t.start || '').slice(0, 10)));
  return active.size;
}