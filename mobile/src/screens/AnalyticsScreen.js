import React, { useCallback, useState } from 'react';
import { View, Text, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, SectionTitle, Empty, Picker, ProgressBar } from '../components/ui';
import { Api, minutesToLabel, todayStr } from '../api/client';

function daysAgo(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

const RANGES = ['7d', '14d', '30d'];

export default function AnalyticsScreen() {
  const [range, setRange] = useState('14d');
  const [data, setData] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const days = Number(range.replace('d', ''));
      const d = await Api.analytics(daysAgo(days - 1), todayStr());
      setData(d);
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  }, [range]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Text style={tw`text-white font-extrabold text-2xl mb-2`}>Analytics</Text>
      <Picker options={RANGES} value={range} onChange={setRange} />
      {data ? <AnalyticsBody data={data} /> : null}
    </Screen>
  );
}

function AnalyticsBody({ data }) {
  const p = data.productivity || {};
  const st = data.study || {};
  const sl = data.sleep || {};
  const ac = data.activities || {};
  const hab = data.habits || {};

  const catTotals = Object.entries(ac.byCategory || {});
  const maxCat = catTotals.reduce((m, [, v]) => Math.max(m, v), 1);

  return (
    <>
      <Row className="gap-3 mb-3">
        <Card className="flex-1">
          <Text style={tw`text-[11px] text-slate-400 uppercase`}>Completion</Text>
          <Text style={tw`text-2xl font-bold text-white mt-1`}>{p.completionRate ?? '—'}%</Text>
          <Text style={tw`text-[11px] text-slate-500`}>{p.totalCompleted}/{p.totalPlanned} tasks</Text>
        </Card>
        <Card className="flex-1">
          <Text style={tw`text-[11px] text-slate-400 uppercase`}>Study</Text>
          <Text style={tw`text-2xl font-bold text-white mt-1`}>{minutesToLabel(st.totalMinutes)}</Text>
          <Text style={tw`text-[11px] text-slate-500`}>{st.sessions || 0} sessions</Text>
        </Card>
      </Row>
      <Row className="gap-3 mb-3">
        <Card className="flex-1">
          <Text style={tw`text-[11px] text-slate-400 uppercase`}>Sleep avg</Text>
          <Text style={tw`text-2xl font-bold text-white mt-1`}>{minutesToLabel(sl.avgDurationMinutes)}</Text>
          <Text style={tw`text-[11px] text-slate-500`}>consistency {sl.consistencyPercent ?? 0}%</Text>
        </Card>
        <Card className="flex-1">
          <Text style={tw`text-[11px] text-slate-400 uppercase`}>Activity</Text>
          <Text style={tw`text-2xl font-bold text-white mt-1`}>{minutesToLabel(ac.totalMinutes)}</Text>
          <Text style={tw`text-[11px] text-slate-500`}>{minutesToLabel(ac.productiveMinutes)} productive</Text>
        </Card>
      </Row>

      <SectionTitle>Study by subject</SectionTitle>
      {Object.keys(st.bySubject || {}).length === 0 && <Empty text="No study data in this range." />}
      {Object.entries(st.bySubject || {}).map(([subj, mins]) => (
        <Card key={subj} className="mb-2 py-2">
          <Row className="justify-between mb-1"><Text style={tw`text-white text-sm`}>{subj}</Text><Text style={tw`text-slate-400 text-sm`}>{minutesToLabel(mins)}</Text></Row>
          <ProgressBar pct={(mins / Math.max(1, Math.max(...Object.values(st.bySubject)))) * 100} color="#38bdf8" />
        </Card>
      ))}

      <SectionTitle>Activity by category</SectionTitle>
      {catTotals.length === 0 && <Empty text="No activities in this range." />}
      {catTotals.map(([cat, mins]) => (
        <Card key={cat} className="mb-2 py-2">
          <Row className="justify-between mb-1"><Text style={tw`text-white text-sm`}>{cat.toLowerCase()}</Text><Text style={tw`text-slate-400 text-sm`}>{minutesToLabel(mins)}</Text></Row>
          <ProgressBar pct={(mins / maxCat) * 100} color="#34d399" />
        </Card>
      ))}

      <SectionTitle>Habits</SectionTitle>
      <Card>
        <Row className="justify-between mb-1">
          <Text style={tw`text-slate-400`}>Check-ins</Text>
          <Text style={tw`text-white font-semibold`}>{hab.actual || 0} / {hab.expected || 0}</Text>
        </Row>
        <ProgressBar pct={hab.expected ? (hab.actual / hab.expected) * 100 : 0} color="#fbbf24" />
      </Card>

      <SectionTitle>Productivity (daily)</SectionTitle>
      <Card>
        {(p.series || []).map((s) => (
          <Row key={s.date} className="justify-between mb-1">
            <Text style={tw`text-slate-400 text-xs`}>{s.date}</Text>
            <Text style={tw`text-slate-300 text-xs`}>
              {s.completed}/{s.planned} {s.missed > 0 ? `· ${s.missed} missed` : ''} {s.completionRate !== null ? `· ${s.completionRate}%` : ''}
            </Text>
          </Row>
        ))}
      </Card>

      <SectionTitle>Sleep (daily)</SectionTitle>
      <Card>
        {(sl.series || []).length === 0 && <Empty text="No sleep records in this range." />}
        {(sl.series || []).map((s) => (
          <Row key={s.date} className="justify-between mb-1">
            <Text style={tw`text-slate-400 text-xs`}>{s.date}</Text>
            <Text style={tw`text-slate-300 text-xs`}>{minutesToLabel(s.durationMinutes)} · {s.quality}</Text>
          </Row>
        ))}
      </Card>
    </>
  );
}