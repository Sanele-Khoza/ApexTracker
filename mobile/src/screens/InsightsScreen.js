import React, { useCallback, useState } from 'react';
import { View, Text, RefreshControl, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Empty } from '../components/ui';
import { Api } from '../api/client';

const typeColor = { task: 'bg-indigo-500', sleep: 'bg-sky-500', study: 'bg-teal-500', habit: 'bg-amber-500', activity: 'bg-emerald-500', general: 'bg-slate-500' };
const typeIcon = { task: '📋', sleep: '😴', study: '📚', habit: '✅', activity: '🏃', general: '💡' };

export default function InsightsScreen() {
  const [items, setItems] = useState([]);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const d = await Api.insights();
      setItems(Array.isArray(d) ? d : []);
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Text style={tw`text-white font-extrabold text-2xl mb-2`}>Insights</Text>
      <Text style={tw`text-slate-400 text-sm mb-3`}>Patterns detected from your tracked data.</Text>
      {items.length === 0 && <Empty text="No insights yet — keep tracking." />}
      {items.map((it, i) => (
        <Card key={i} className="mb-2 py-3 flex-row items-center">
          <Text style={tw`text-xl mr-3`}>{typeIcon[it.type] || '💡'}</Text>
          <View style={tw`flex-1`}>
            <Text style={tw`${typeColor[it.type] || 'bg-slate-500'} text-white text-[10px] font-semibold px-2 py-0.5 rounded-full overflow-hidden self-start mb-1`}>{it.type.toUpperCase()}</Text>
            <Text style={tw`text-slate-200 text-sm`}>{it.text}</Text>
          </View>
        </Card>
      ))}
    </Screen>
  );
}