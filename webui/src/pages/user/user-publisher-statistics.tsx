/********************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent } from 'react';
import { PublisherStatistic } from '../../extension-registry-types';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Paper, Stack, Typography, useMediaQuery, useTheme } from '@mui/material';
import { red, pink, purple, deepPurple, indigo, blue, lightBlue, cyan, teal, green, lightGreen, yellow, orange, deepOrange, brown, blueGrey } from '@mui/material/colors';

interface UserPublisherStatisticsProps {
    statistics: PublisherStatistic[];
}

const colors = [
    red[500],
    cyan[500],
    pink[500],
    teal[500],
    purple[500],
    green[500],
    deepPurple[500],
    lightGreen[500],
    indigo[500],
    yellow[500],
    blue[500],
    orange[500],
    lightBlue[500],
    deepOrange[500],
    brown[500],
    blueGrey[500]
];

interface BarChartCardProps {
    title: string
    data: Array<Record<string, number | string>>
    bars: Set<string>
}

const BarChartCard: FunctionComponent<BarChartCardProps> = props => {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
    return <Paper elevation={3} sx={{ p: 2 }}>
        <Typography variant='h6' align='center'>{props.title}</Typography>
        <ResponsiveContainer height={300}>
            <BarChart
                data={props.data}
            >
                <CartesianGrid strokeDasharray='3 3' />
                <XAxis dataKey='name' />
                <YAxis />
                <Tooltip
                    cursor={false}
                    contentStyle={{ backgroundColor: theme.palette.background.paper }}
                    itemStyle={{ color: theme.palette.text.primary }} />
                <Legend
                    layout={isMobile ? 'horizontal' : 'vertical'}
                    verticalAlign={isMobile ? 'bottom' : 'middle'}
                    align='left'
                />
                {
                    [...props.bars].map((value, index) => <Bar key={value} dataKey={value} stackId='a' fill={colors[index % colors.length]} />)
                }
            </BarChart>
        </ResponsiveContainer>
    </Paper>;
};

export const UserPublisherStatistics: FunctionComponent<UserPublisherStatisticsProps> = props => {

    const relativeDownloads: Record<string, number | string>[] = [];
    const relativeSeries = new Set<string>();
    const totalDownloads: Record<string, number | string>[] = [];
    const totalSeries = new Set<string>();
    for (const statistic of props.statistics) {
        const { month, year, extensionDownloads, extensionTotalDownloads } = statistic;
        const download: Record<string, number | string> = { name: `${month}/${year}` };
        for (const extensionDownload of extensionDownloads) {
            const { extensionIdentifier, downloads } = extensionDownload;
            download[extensionIdentifier] = downloads;
            relativeSeries.add(extensionIdentifier);
        }
        relativeDownloads.push(download);

        const totalDownload: Record<string, number | string> = { name: `${month}/${year}` };
        for (const extensionDownload of extensionTotalDownloads) {
            const { extensionIdentifier, downloads } = extensionDownload;
            totalDownload[extensionIdentifier] = downloads;
            totalSeries.add(extensionIdentifier);
        }
        totalDownloads.push(totalDownload);
    }

    return <Stack spacing={2}>
        <BarChartCard title='Monthly Downloads' data={relativeDownloads} bars={relativeSeries} />
        <BarChartCard title='Total Downloads' data={totalDownloads} bars={totalSeries} />
    </Stack>;
};

