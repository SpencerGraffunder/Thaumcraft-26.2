#!/usr/bin/env python3
"""Probe: ScreenCast session -> Start -> RecordVirtual -> inspect stream."""
import gi
gi.require_version('Gio', '2.0')
gi.require_version('GLib', '2.0')
from gi.repository import Gio, GLib

conn = Gio.bus_get_sync(Gio.BusType.SESSION, None)

def call(dest, path, iface, method, var=None):
    res = conn.call_sync(dest, path, iface, method, var, None,
                         Gio.DBusCallFlags.NONE, -1, None)
    return res.unpack()

SC = "org.gnome.Mutter.ScreenCast"
SIF = SC + ".Session"

def introspect(path):
    return call(SC, path, "org.freedesktop.DBus.Introspectable", "Introspect")[0]

(spath,) = call(SC, "/org/gnome/Mutter/ScreenCast", SC, "CreateSession",
                GLib.Variant("(a{sv})", ({},)))
print("SC session:", spath, flush=True)

print("Start:", call(SC, spath, SIF, "Start"), flush=True)

(sp,) = call(SC, spath, SIF, "RecordVirtual",
             GLib.Variant("(a{sv})", ({},)))
print("stream:", sp, flush=True)
print(introspect(sp), flush=True)
