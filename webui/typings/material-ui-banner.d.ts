declare module 'material-ui-banner' {
    import React from 'react';
    import { PaperProps, CardProps } from '@material-ui/core';

    export interface BannerProps {
        open: boolean,
        label: string,
        buttonLabel?: string,
        buttonOnClick?: (event: React.MouseEvent<HTMLAnchorElement, MouseEvent>) => void,
        buttonComponent?: any,
        buttonProps?: object,
        showDismissButton?: boolean,
        dismissButtonLabel?: string,
        dismissButtonProps?: object,
        onClose?: (event: React.MouseEvent<HTMLAnchorElement, MouseEvent>) => void,
        icon?: React.ReactNode,
        iconProps?: object,
        appBar?: boolean,
        paperProps?: PaperProps,
        cardProps?: CardProps
    }
    export const Banner: React.NamedExoticComponent<BannerProps>;
}