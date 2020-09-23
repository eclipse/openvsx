import React, { FunctionComponent } from 'react';
import { SearchListContainer } from './search-list-container';
import { ExtensionListSearchfield } from '../extension-list/extension-list-searchfield';
import { Box } from '@material-ui/core';

export const ExtensionAdmin: FunctionComponent = props => {
    const handleSearchChange = (value: string) => {
        console.log(value);
    };
    return (<>
        <SearchListContainer
            searchContainer={
                <ExtensionListSearchfield onSearchChanged={handleSearchChange} />
            }
            listContainer={
                <Box>A List of Extensions</Box>
            }
        />
    </>);
};