import React, { useCallback, useState } from 'react';
import { View, Text, Pressable, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Btn, Empty, Field, Modal, ErrorBox } from '../components/ui';
import { Api, minutesToLabel, todayStr } from '../api/client';

export default function SleepScreen() {
  const [data, setData] = useState(null);
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState({ date: todayStr(), sleepTime: '22:30', wakeTime: '06:30', quality: 'GOOD', mood: '' });
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const d = await Api.sleepRecords();
      setData(d);
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const save = async () => {
    setError('');
    if (!form.sleepTime || !form.wakeTime) return setError('Sleep and wake times are required');
    try {
      await Api.createSleep(form);
      setModal(false);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Row className="justify-between mb-2">
        <Text style={tw`text-white font-extrabold text-2xl`}>Sleep</Text>
        <Btn title="+ Record" onPress={() => { setForm({ date: todayStr(), sleepTime: '22:30', wakeTime: '06:30', quality: 'GOOD', mood: '' }); setError(''); setModal(true); }} />
      </Row>

      {data && (
        <>
          <View style={tw`flex-row gap-3 mb-4`}>
            <Card className="flex-1">
              <Text style={tw`text-[11px] text-slate-400 uppercase`}>Last night</Text>
              <Text style={tw`text-2xl font-bold text-white mt-1 text-sky-400`}>{minutesToLabel(data.lastNightMinutes)}</Text>
            </Card>
            <Card className="flex-1">
              <Text style={tw`text-[11px] text-slate-400 uppercase`}>This week</Text>
              <Text style={tw`text-2xl font-bold text-white mt-1`}>{minutesToLabel(data.weekMinutes)}</Text>
            </Card>
            <Card className="flex-1">
              <Text style={tw`text-[11px] text-slate-400 uppercase`}>This month</Text>
              <Text style={tw`text-2xl font-bold text-white mt-1`}>{minutesToLabel(data.monthMinutes)}</Text>
            </Card>
          </View>
          {data.records && data.records.length === 0 && <Empty text="No sleep records yet." />}
          {data.records && data.records.map((r) => (
            <Card key={r.id} className="mb-2 py-3">
              <Row className="justify-between mb-1">
                <Text style={tw`text-white font-semibold`}>{r.date}</Text>
                <QualityBadge q={r.quality} />
              </Row>
              <Text style={tw`text-slate-400 text-xs`}>
                {r.sleepTime} – {r.wakeTime} · {minutesToLabel(r.durationMinutes)}
              </Text>
              {r.mood ? <Text style={tw`text-slate-500 text-xs`}>Mood: {r.mood}</Text> : null}
              <Row className="gap-3 mt-1">
                <Pressable onPress={() => Alert.alert('Delete record', 'Delete sleep record for ' + r.date + '?', [
                  { text: 'Cancel', style: 'cancel' },
                  { text: 'Delete', style: 'destructive', onPress: async () => { await Api.deleteSleep(r.id); load(); } },
                ])}><Text style={tw`text-rose-400 text-xs`}>Delete</Text></Pressable>
                <Pressable onPress={() => Alert.alert('Info', 'Use edit via re-recording on the same date, or delete and re-enter.')}><Text style={tw`text-indigo-400 text-xs`}>i</Text></Pressable>
              </Row>
            </Card>
          ))}
        </>
      )}

      <Modal visible={modal} onClose={() => setModal(false)} title="Record sleep">
        <ErrorBox message={error} />
        <Field label="Date (morning wake date)" value={form.date} onChangeText={(v) => setForm({ ...form, date: v })} placeholder="YYYY-MM-DD" />
        <Row className="gap-2">
          <View style={tw`flex-1`}><Field label="Sleep time" value={form.sleepTime} onChangeText={(v) => setForm({ ...form, sleepTime: v })} placeholder="HH:MM" /></View>
          <View style={tw`flex-1`}><Field label="Wake time" value={form.wakeTime} onChangeText={(v) => setForm({ ...form, wakeTime: v })} placeholder="HH:MM" /></View>
        </Row>
        <Text style={tw`text-xs text-slate-400 mb-1 ml-1`}>Quality</Text>
        <View style={tw`flex-row gap-2 mb-3`}>
          {['GOOD', 'FAIR', 'POOR'].map((q) => (
            <Pressable key={q} onPress={() => setForm({ ...form, quality: q })}
              style={tw`px-3 py-1.5 rounded-full border ${form.quality === q ? 'bg-sky-600 border-sky-500' : 'bg-slate-800 border-slate-700'}`}>
              <Text style={tw`text-xs ${form.quality === q ? 'text-white font-semibold' : 'text-slate-300'}`}>{q}</Text>
            </Pressable>
          ))}
        </View>
        <Field label="Mood (optional)" value={form.mood} onChangeText={(v) => setForm({ ...form, mood: v })} placeholder="e.g. Rested" />
        <Btn title="Save" onPress={save} />
      </Modal>
    </Screen>
  );
}

function QualityBadge({ q }) {
  const c = q === 'GOOD' ? 'bg-emerald-600' : q === 'FAIR' ? 'bg-amber-600' : 'bg-rose-600';
  return <Text style={tw`${c} text-white text-[10px] font-semibold px-2 py-0.5 rounded-full overflow-hidden`}>{q}</Text>;
}