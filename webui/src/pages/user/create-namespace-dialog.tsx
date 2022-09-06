/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

 import * as React from 'react';
 import {
     Button, Theme, createStyles, WithStyles, withStyles, Dialog, DialogTitle,
     DialogContent, Box, TextField, DialogActions
 } from '@material-ui/core';
 import { ButtonWithProgress } from '../../components/button-with-progress';
 import { isError } from '../../extension-registry-types';
 import { MainContext } from '../../context';

 const NAMESPACE_NAME_SIZE = 255;

 const namespaceDialogStyle = (theme: Theme) => createStyles({});

 class CreateNamespaceDialogComponent extends React.Component<CreateNamespaceDialogComponent.Props, CreateNamespaceDialogComponent.State> {

     static contextType = MainContext;
     declare context: MainContext;

     protected abortController = new AbortController();

     constructor(props: CreateNamespaceDialogComponent.Props) {
         super(props);

         this.state = {
             open: false,
             posted: false,
             name: ''
         };
     }

     protected handleOpenDialog = () => {
         this.setState({ open: true, posted: false });
     };

     protected handleCancel = () => {
         this.setState({
             open: false,
             name: ''
         });
     };

     protected handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
         const name = event.target.value;
         let nameError: string | undefined;
         if (name.length > NAMESPACE_NAME_SIZE) {
             nameError = `The namespace name must not be longer than ${NAMESPACE_NAME_SIZE} characters.`;
         }
         this.setState({ name, nameError });
     };

     protected handleCreateNamespace = async () => {
         if (!this.context.user) {
             return;
         }
         this.setState({ posted: true });
         try {
             const response = await this.context.service.createNamespace(this.abortController, this.state.name);
             if (isError(response)) {
                 throw response;
             }

             this.setState({ open: false });
             this.props.namespaceCreated();
         } catch (err) {
             this.context.handleError(err);
         }

         this.setState({ posted: false });
     };

     handleEnter = (e: KeyboardEvent) => {
         if (e.code ===  'Enter') {
             this.handleCreateNamespace();
         }
     };

     componentDidMount() {
         document.addEventListener('keydown', this.handleEnter);
     }

     componentWillUnmount() {
         document.removeEventListener('keydown', this.handleEnter);
     }

     render() {
         return <React.Fragment>
             <Button variant='outlined' onClick={this.handleOpenDialog}>Create namespace</Button>
             <Dialog open={this.state.open} onClose={this.handleCancel}>
                 <DialogTitle>Create new namespace</DialogTitle>
                 <DialogContent>
                     <Box my={2}>
                         <TextField
                             fullWidth
                             label='Namespace Name'
                             error={Boolean(this.state.nameError)}
                             helperText={this.state.nameError}
                             onChange={this.handleNameChange} />
                     </Box>
                 </DialogContent>
                 <DialogActions>
                     <Button onClick={this.handleCancel} color='secondary'>
                         Cancel
                     </Button>
                    <ButtonWithProgress
                            autoFocus
                            error={Boolean(this.state.nameError) || !this.state.name}
                            working={this.state.posted}
                            onClick={this.handleCreateNamespace} >
                        Create Namespace
                    </ButtonWithProgress>
                 </DialogActions>
             </Dialog>
         </React.Fragment>;
     }
 }

 export namespace CreateNamespaceDialogComponent {
     export interface Props extends WithStyles<typeof namespaceDialogStyle> {
        namespaceCreated: () => void;
     }

     export interface State {
         open: boolean;
         posted: boolean;
         name: string;
         nameError?: string;
     }
 }

 export const CreateNamespaceDialog = withStyles(namespaceDialogStyle)(CreateNamespaceDialogComponent);