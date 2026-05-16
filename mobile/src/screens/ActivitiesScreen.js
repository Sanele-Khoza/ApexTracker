import React, { useCallback, useState, useEffect } from 'react';
import { View, Text, Pressable, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Btn, Badge, Empty, Field, Picker, Modal, ErrorBox } from '../components/ui';
import { Api, minutesToLabel } from '../api/client';

const CATEGORIES = ['STUDYING', 'CODING', 'READING', 'EXERCISING', 'WORKING', 'WATCHING_VIDEOS', 'GAMING', 'RESTING', 'PERSONAL', 'OTHER'];
const catColor = { STUDYING: 'bg-indigo-600', CODING: 'bg-violet-600', READING: 'bg-teal-600', EXERCISING: 'bg-emerald-600', WORKING: 'bg-sky-600', WATCHING_VIDEOS: 'bg-amber-600', GAMING: 'bg-purple-600', RESTING: 'bg-slate-600', PERSONAL: 'bg-pink-600', OTHER: 'bg-slate-700' };

export default function ActivitiesScreen() {
  const [items, setItems] = useState([]);
  const [modal, setModal] = useState(false);
  const [name, setName] = useState('');
  const [category, setCategory] = useState('CODING');
  const [seconds, setSeconds] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await Api.activities();
      setItems(data);
      const active = data.find((a) => a.active);
      if (active) setSeconds(Math.round((Date.now() - new Date(active.startTime).getTime()) / 1000));
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  useEffect(() => {
    const id = setInterval(() => {
      setSeconds((s) => s + 1);
    }, 1000);
    return () => clearInterval(id);
  }, []);

  const active = items.find((a) => a.active);

  const start = async () => {
    if (!name.trim()) return;
    try {
      await Api.startActivity({ name, category });
      setModal(false);
      setName('');
      load();
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  };

  const fmt = (sec) => {
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Row className="justify-between mb-2">
        <Text style={tw`text-white font-extrabold text-2xl`}>Activities</Text>
        <Btn title="+ Track" onPress={() => setModal(true)} />
      </Row>

      <Card className={`mb-4 ${active ? 'border-emerald-600' : 'border-slate-800'}`}>
        {active ? (
          <>
            <Text style={tw`text-emerald-400 text-xs font-semibold uppercase`}>● Recording</Text>
            <Text style={tw`text-white text-lg font-bold mt-1`}>{active.name}</Text>
            <Text style={tw`text-slate-400 text-xs`}>{active.category} · started {new Date(active.startTime).toLocaleTimeString()}</Text>
            <Text style={tw`text-white text-3xl font-bold mt-3 text-center`}>{fmt(seconds)}</Text>
            <Btn title="■ Stop & save" variant="success" className="mt-3" onPress={async () => { await Api.stopActivity(active.id); load(); }} />
          </>
        ) : (
          <>
            <Text style={tw`text-slate-400`}>No activity running.</Text>
            <Text style={tw`text-xs text-slate-500 mt-1`}>Start a timer to record what you actually spend time doing.</Text>
          </>
        )}
      </Card>

      {items.filter((a) => !a.active).length === 0 && <Empty text="No completed activities yet." />}
      {items.filter((a) => !a.active).map((a) => (
        <Card key={a.id} className="mb-2 py-3">
          <Row className="justify-between">
            <View style={tw`flex-1`}>
              <Row className="gap-2 mb-1"><Badge color={catColor[a.category] || 'bg-slate-700'}>{a.category}</Badge></Row>
              <Text style={tw`text-white font-semibold`}>{a.name}</Text>
              <Text style={tw`text-xs text-slate-500`}>
                {a.startTime ? new Date(a.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''} –{' '}
                {a.endTime ? new Date(a.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''} · {minutesToLabel(a.durationMinutes)}
              </Text>
              <Text style={tw`text-xs text-slate-500`}>{a.date}</Text>
            </View>
            <Pressable onPress={() => Alert.alert('Delete activity', 'Delete ' + a.name + '?', [
              { text: 'Cancel', style: 'cancel' },
              { text: 'Delete', style: 'destructive', onPress: async () => { await Api.deleteActivity(a.id); load(); } },
            ])}><Text style={tw`text-rose-400 text-xs`}>Delete</Text></Pressable>
          </Row>
        </Card>
      ))}

      <Modal visible={modal} onClose={() => setModal(false)} title="Start an activity">
        <Field label="Name" value={name} onChangeText={setName} placeholder="e.g. Coding, Reading, Gym" />
        <Text style={tw`text-xs text-slate-400 mb-1 ml-1`}>Category</Text>
        <Picker options={CATEGORIES} value={category} onChange={setCategory} />
        <Btn title="Start timer" onPress={start} />
      </Modal>
    </Screen>
  );
}