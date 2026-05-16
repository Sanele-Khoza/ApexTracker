import React, { useCallback, useState } from 'react';
import { View, Text, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Empty, Field, Btn, ProgressBar, SectionTitle } from '../components/ui';
import { Api, minutesToLabel, todayStr } from '../api/client';

export default function PlanActualScreen() {
  const [date, setDate] = useState(todayStr());
  const [data, setData] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    if (!date) return;
    try {
      setData(await Api.planVsActual(date));
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  }, [date]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Text style={tw`text-white font-extrabold text-2xl mb-2`}>Plan vs Actual</Text>
      <Text style={tw`text-slate-400 text-sm mb-3`}>Planned minutes (tasks) against what you actually tracked.</Text>
      <Field label="Date" value={date} onChangeText={setDate} placeholder="YYYY-MM-DD" />

      {data && (
        <>
          <SectionTitle>{data.date}</SectionTitle>
          {data.rows.map((r) => {
            const planned = r.plannedMinutes || 0;
            const actual = r.actualMinutes || 0;
            const diff = actual - planned;
            const max = Math.max(planned, actual, 1);
            return (
              <Card key={r.category} className="mb-3">
                <Row className="justify-between mb-1">
                  <Text style={tw`text-white font-semibold`}>{r.category}</Text>
                  <Text style={tw`text-slate-400 text-sm`}>
                    planned {minutesToLabel(planned)} · actual {minutesToLabel(actual)}
                  </Text>
                </Row>
                <ProgressBar pct={(planned / max) * 100} color="#475569" />
                <ProgressBar pct={(actual / max) * 100} color={diff >= 0 ? '#34d399' : '#fb7185'} />
                <Text style={tw`text-xs mt-1 ${diff >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {diff >= 0 ? '+' : ''}{minutesToLabel(diff)} {diff >= 0 ? 'ahead' : 'behind'}
                </Text>
              </Card>
            );
          })}
          {data.rows.length === 0 && <Empty text="No plan data for this date." />}
        </>
      )}
    </Screen>
  );
}