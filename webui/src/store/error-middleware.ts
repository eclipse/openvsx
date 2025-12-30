import { isFulfilled, isRejectedWithValue, Middleware, MiddlewareAPI } from "@reduxjs/toolkit";
import { isError, ReportedError } from "../extension-registry-types";
import { ErrorPayload, setError } from "./error";
import { handleError } from "../utils";

const getEndpointName = (action: any) => {
    let meta;
    if ('meta' in action) {
        meta = action.meta;
    }
    let arg;
    if (meta != null && 'arg' in meta) {
        arg = meta.arg;
    }

    return arg != null && 'endpointName' in arg ? arg.endpointName : '';
};

export const errorMiddleware: Middleware =
    (api: MiddlewareAPI) => (next) => (action) => {
        console.log('MW', action);
        let error: ErrorPayload | undefined = undefined;
        if (isRejectedWithValue(action)) {
            // TODO handle rejected
            console.error('rejected');
        } else if (isFulfilled(action) && isError(action.payload)) {
            error = { code: '', message: handleError(action.payload) };
        }

        if (error != null && getEndpointName(action) === 'signPublisherAgreement' && !(error as ReportedError).code) {
            error.code = 'publisher-agreement-problem';
        }
        if (error != null) {
            api.dispatch(setError(error));
        }
        return next(action);
    };