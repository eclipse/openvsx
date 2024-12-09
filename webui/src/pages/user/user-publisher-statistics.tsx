/********************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useMemo } from 'react';
import { PublisherStatistics } from '../../extension-registry-types';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Paper, Stack, Typography, useMediaQuery, useTheme } from '@mui/material';
import { red, pink, purple, deepPurple, indigo, blue, lightBlue, cyan, teal, green, lightGreen, yellow, orange, deepOrange, brown, blueGrey } from '@mui/material/colors';

interface UserPublisherStatisticsProps {
    statistics: PublisherStatistics[];
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
    bars: string[]
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

export const UserPublisherStatistics: FunctionComponent<UserPublisherStatisticsProps> = ({ statistics }) => {

    const charts = useMemo((): BarChartCardProps[] => {
        const relativeSeries = new Set<string>();
        const absoluteSeries = new Set<string>();
        const relativeDownloads: Array<Record<string, string | number>> = [];
        const absoluteDownloads: Array<Record<string, string | number>> = [];

        for (const stat of statistics) {
            const { year, month, extensionDownloads, extensionTotalDownloads } = stat;
            const name = `${month}/${year}`;
            relativeDownloads.push({ ...extensionDownloads, name });
            absoluteDownloads.push({ ...extensionTotalDownloads, name });
            Object.keys(extensionDownloads).forEach((key) => relativeSeries.add(key));
            Object.keys(extensionTotalDownloads).forEach((key) => absoluteSeries.add(key));
        }

        return [
            { title: 'Relative Downloads', data: relativeDownloads, bars: Array.from(relativeSeries).sort() },
            { title: 'Absolute Downloads', data: absoluteDownloads, bars: Array.from(absoluteSeries).sort() }
        ];
    }, [statistics]);



    return <Stack spacing={2}>
        {
            charts.map(({ title, data, bars }) => <BarChartCard key={title} title={title} data={data} bars={bars} />)
        }
    </Stack>;
};

