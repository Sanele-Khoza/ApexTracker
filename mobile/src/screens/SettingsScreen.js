import React, { useCallback, useState } from 'react';
import { View, Text, Pressable, RefreshControl, Alert, ScrollView } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import tw from 'twrnc';
import { Screen, Card, Row, Btn, Field, ErrorBox, Modal } from '../components/ui';
import { Api } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function SettingsScreen() {
  const { user, logout } = useAuth();
  const [settings, setSettings] = useState(null);
  const [exportData, setExportData] = useState(null);
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      setSettings(await Api.settings());
    } catch (e) {
      setError(e.message);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const set = (k, v) => setSettings((s) => ({ ...s, [k]: v }));

  const save = async () => {
    try {
      const res = await Api.updateSettings({
        weekStartDay: settings.weekStartDay,
        bedTimeTarget: settings.bedTimeTarget,
        wakeTimeTarget: settings.wakeTimeTarget,
        weeklyStudyGoalMinutes: settings.weeklyStudyGoalMinutes,
        quietHoursStart: settings.quietHoursStart,
        quietHoursEnd: settings.quietHoursEnd,
        timezone: settings.timezone,
        theme: settings.theme,
        reminderDefaultMinutes: settings.reminderDefaultMinutes,
        reminderEnabled: settings.reminderEnabled,
        emailNotifications: settings.emailNotifications,
      });
      setSettings(res);
      Alert.alert('Saved', 'Settings updated.');
    } catch (e) {
      setError(e.message);
    }
  };

  const doExport = async () => {
    try {
      const d = await Api.exportData();
      setExportData(d);
    } catch (e) {
      Alert.alert('Error', e.message);
    }
  };

  const doDelete = () => {
    Alert.alert('Delete account', 'This permanently deletes all your data. Continue?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete everything', style: 'destructive', onPress: async () => { await Api.deleteAccount(); logout(); } },
    ]);
  };

  if (!settings) return <Screen scroll={false}><Text style={tw`text-slate-400 text-center mt-10`}>Loading…</Text></Screen>;

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load().finally(() => setRefreshing(false)); }} />}>
      <Text style={tw`text-white font-extrabold text-2xl mb-2`}>Settings</Text>
      <ErrorBox message={error} />

      <Card className="mb-3">
        <Text style={tw`text-white font-bold`}>{user?.name}</Text>
        <Text style={tw`text-slate-400 text-sm`}>{user?.email}</Text>
        <Text style={tw`text-slate-500 text-xs mt-1`}>timezone {settings.timezone}</Text>
      </Card>

      <Text style={tw`text-slate-400 font-bold mb-1 mt-2`}>Schedule targets</Text>
      <Card className="mb-3">
        <Row className="gap-2">
          <View style={tw`flex-1`}><Field label="Bed time (HH:MM)" value={settings.bedTimeTarget} onChangeText={(v) => set('bedTimeTarget', v)} /></View>
          <View style={tw`flex-1`}><Field label="Wake time (HH:MM)" value={settings.wakeTimeTarget} onChangeText={(v) => set('wakeTimeTarget', v)} /></View>
        </Row>
        <Field label="Weekly study goal (minutes)" keyboardType="numeric" value={String(settings.weeklyStudyGoalMinutes || 0)} onChangeText={(v) => set('weeklyStudyGoalMinutes', Number(v) || 0)} />
        <Row className="gap-2">
          <View style={tw`flex-1`}><Field label="Quiet hours start" value={settings.quietHoursStart} onChangeText={(v) => set('quietHoursStart', v)} /></View>
          <View style={tw`flex-1`}><Field label="Quiet hours end" value={settings.quietHoursEnd} onChangeText={(v) => set('quietHoursEnd', v)} /></View>
        </Row>
        <Text style={tw`text-xs text-slate-400 mb-1 ml-1`}>Week starts on</Text>
        <View style={tw`flex-row gap-2 mb-3`}>
          {['MONDAY', 'SUNDAY'].map((d) => (
            <Pressable key={d} onPress={() => set('weekStartDay', d)}
              style={tw`px-3 py-1.5 rounded-full border ${settings.weekStartDay === d ? 'bg-indigo-600 border-indigo-500' : 'bg-slate-800 border-slate-700'}`}>
              <Text style={tw`text-xs ${settings.weekStartDay === d ? 'text-white font-semibold' : 'text-slate-300'}`}>{d}</Text>
            </Pressable>
          ))}
        </View>
        <Row className="justify-between mb-3">
          <Text style={tw`text-slate-300 text-sm`}>Reminders enabled</Text>
          <Toggle on={settings.reminderEnabled} onToggle={(v) => set('reminderEnabled', v)} />
        </Row>
        <Row className="justify-between mb-3">
          <Text style={tw`text-slate-300 text-sm`}>Email notifications</Text>
          <Toggle on={settings.emailNotifications} onToggle={(v) => set('emailNotifications', v)} />
        </Row>
        <Btn title="Save settings" onPress={save} className="mt-1" />
      </Card>

      <Btn title="Export my data (JSON)" variant="ghost" onPress={doExport} className="mb-3" />
      <Btn title="Log out" variant="ghost" onPress={logout} className="mb-3" />
      <Btn title="Delete my account" variant="danger" onPress={doDelete} />

      <Modal visible={!!exportData} onClose={() => setExportData(null)} title="Exported data">
        <ScrollView>
          <Text style={tw`text-slate-300 text-xs font-mono`}>{JSON.stringify(exportData, null, 2)}</Text>
        </ScrollView>
      </Modal>
    </Screen>
  );
}

function Toggle({ on, onToggle }) {
  return (
    <Pressable onPress={() => onToggle(!on)} style={tw`w-12 h-7 rounded-full ${on ? 'bg-indigo-600' : 'bg-slate-700'} justify-center px-1`}>
      <View style={tw`w-5 h-5 rounded-full bg-white ${on ? 'self-end' : 'self-start'}`} />
    </Pressable>
  );
}