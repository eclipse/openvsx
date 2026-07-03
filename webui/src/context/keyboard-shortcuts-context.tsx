/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import {
    createContext,
    FunctionComponent,
    PropsWithChildren,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useRef,
    useState
} from 'react';

export interface ShortcutInfo {
    key: string;
    label: string;
    order: number;
}

interface ShortcutEntry extends ShortcutInfo {
    callback: () => void;
}

interface KeyboardShortcutsContextValue {
    shortcuts: ShortcutInfo[];
    register: (entry: ShortcutEntry) => void;
    unregister: (key: string) => void;
}

const KeyboardShortcutsContext = createContext<KeyboardShortcutsContextValue>({
    shortcuts: [],
    register: () => {},
    unregister: () => {}
});

// eslint-disable-next-line react-refresh/only-export-components
export const useKeyboardShortcuts = () => useContext(KeyboardShortcutsContext);

export const KeyboardShortcutsProvider: FunctionComponent<PropsWithChildren> = ({ children }) => {
    const registryRef = useRef<Map<string, ShortcutEntry>>(new Map());
    const [shortcuts, setShortcuts] = useState<ShortcutInfo[]>([]);

    const sync = () =>
        setShortcuts(
            Array.from(registryRef.current.values())
                .map(({ key, label, order }) => ({ key, label, order }))
                .sort((a, b) => a.order - b.order)
        );

    const register = useCallback((entry: ShortcutEntry) => {
        registryRef.current.set(entry.key, entry);
        sync();
    }, []);

    const unregister = useCallback((key: string) => {
        registryRef.current.delete(key);
        sync();
    }, []);

    // Chord detection: only fire a shortcut when the key was pressed in isolation.
    //
    // Two subtleties drive the implementation:
    //
    // 1. Key identity: Shift changes e.key (Shift+/ → e.key='?'), but e.code is
    //    always the physical key ('Slash'). We track by e.code and store e.key at
    //    keydown so the correct character is available at keyup even if Shift was
    //    released in between.
    //
    // 2. Browser defaults: some browsers handle '/' as a find shortcut on keydown,
    //    before our keyup fires. When a solo registered key goes down we eagerly
    //    call e.preventDefault() to block that, then fire the callback on keyup.
    //
    // 3. Missed keyups: browsers don't always deliver keyup. On macOS a held Meta
    //    swallows the keyup of the other key (Cmd+C, Cmd+T, …), and losing window
    //    focus (Alt+Tab, opening a tab) sends the keyup elsewhere. Either leaves
    //    stale entries in `pressed`/`dirty` that permanently break detection, so we
    //    flush the transient state on window blur and when Meta is released.
    useEffect(() => {
        const pressed = new Map<string, string>(); // code → key captured at keydown
        const dirty = new Set<string>(); // codes involved in a chord

        const reset = () => {
            pressed.clear();
            dirty.clear();
        };

        // Ignore keys typed into inputs and keys pressed while a popup owns focus
        // (MUI selects/menus/dialogs render as listbox/menu/dialog roles, not native elements).
        const shouldIgnore = (target: EventTarget | null) => {
            const el = target as HTMLElement;
            return (
                el.tagName === 'INPUT' ||
                el.tagName === 'TEXTAREA' ||
                el.tagName === 'SELECT' ||
                el.isContentEditable ||
                Boolean(el.closest?.('[role="listbox"], [role="menu"], [role="dialog"]'))
            );
        };

        const onKeydown = (e: KeyboardEvent) => {
            if (e.repeat || e.key === 'Shift') return;
            if (pressed.size > 0) {
                pressed.forEach((_, code) => dirty.add(code));
                dirty.add(e.code);
            } else if (!shouldIgnore(e.target) && registryRef.current.has(e.key)) {
                e.preventDefault();
            }
            pressed.set(e.code, e.key);
        };

        const onKeyup = (e: KeyboardEvent) => {
            if (e.key === 'Shift') return;
            // Meta chords swallow the keyup of the other key, so once Meta is
            // released flush anything still held to avoid stale state.
            if (e.key === 'Meta') return reset();
            const isDirty = dirty.has(e.code);
            const key = pressed.get(e.code) ?? e.key;
            pressed.delete(e.code);
            dirty.delete(e.code);
            if (isDirty || shouldIgnore(e.target)) return;
            const entry = registryRef.current.get(key);
            if (entry) entry.callback();
        };

        document.addEventListener('keydown', onKeydown);
        document.addEventListener('keyup', onKeyup);
        window.addEventListener('blur', reset);
        return () => {
            document.removeEventListener('keydown', onKeydown);
            document.removeEventListener('keyup', onKeyup);
            window.removeEventListener('blur', reset);
        };
    }, []);

    return (
        <KeyboardShortcutsContext.Provider
            value={useMemo(() => ({ shortcuts, register, unregister }), [shortcuts, register, unregister])}>
            {children}
        </KeyboardShortcutsContext.Provider>
    );
};
