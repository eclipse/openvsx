import { configureStore } from '@reduxjs/toolkit';
import { apiSlice } from './api';
import errorReducer from './error';
import { errorMiddleware } from './error-middleware';

export const store = configureStore({
  reducer: {
    [apiSlice.reducerPath]: apiSlice.reducer,
    error: errorReducer
  },
  middleware: getDefaultMiddleware =>
    getDefaultMiddleware().concat(apiSlice.middleware, errorMiddleware)
});

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
export type AppStore = typeof store