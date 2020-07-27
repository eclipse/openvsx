FROM node:12-alpine3.12 as builder

# Copy sources
COPY --chown=node:node package.json tsconfig.json yarn.lock /home/node/builder/
WORKDIR /home/node/builder
COPY --chown=node:node configs ./configs/
COPY --chown=node:node src ./src/
COPY --chown=node:node static ./static/
COPY --chown=node:node test ./test/

# Build and test the library and default app
RUN yarn install --frozen-lockfile
RUN yarn test
RUN yarn build:default


FROM node:12-alpine3.12

# Copy build result to runtime directory
COPY --chown=node:node --from=builder /home/node/builder/static /home/node/webui/static/
COPY --chown=node:node --from=builder /home/node/builder/lib/default/server.js /home/node/webui/lib/default/
WORKDIR /home/node/webui

# Install Express server
RUN yarn add express@4.17.1

EXPOSE 3000
ENTRYPOINT ["node", "lib/default/server"]
