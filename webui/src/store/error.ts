import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from './store';
import { apiSlice } from './api';
import { handleError } from '../utils';

export interface ErrorPayload {
    code: string
    message: string
}

export interface ErrorState extends ErrorPayload {
    show: boolean
}

const initialState: ErrorState = {
    code: '',
    message: '',
    show: false
};

export const errorSlice = createSlice({
    name: 'error',
    initialState,
    reducers: {
        setError: (state: ErrorState, action: PayloadAction<ErrorPayload>) => {
            state = { ...action.payload, show: true };
        },
        hideError: (state: ErrorState) => {
            state.show = false;
        }
    },
    extraReducers(builder) {
        builder.addMatcher(apiSlice.endpoints.getUserAuthError.matchFulfilled, (state, action) => {
            state = {
                code: '',
                message: handleError(action.payload),
                show: true
            };
        });
    },
});

export const { setError, hideError } = errorSlice.actions;

export const selectError = (state: RootState) => state.error;

export default errorSlice.reducer;