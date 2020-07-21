FROM node:12-alpine3.12 as builder

USER node
RUN mkdir -p /home/node/builder/configs /home/node/builder/src /home/node/builder/dev && chown -R node:node /home/node/builder
WORKDIR /home/node/builder

COPY --chown=node:node src src/
COPY --chown=node:node dev dev/
COPY --chown=node:node package.json yarn.lock tsconfig.json ./
COPY --chown=node:node ./configs/base.tsconfig.json ./configs/webpack.config.js configs/

RUN yarn install --frozen-lockfile --ignore-scripts
RUN yarn run build:dev


FROM node:12-alpine3.12

USER node
RUN mkdir -p /home/node/webui/dev/server /home/node/webui/dev/static && chown -R node:node /home/node/webui
WORKDIR /home/node/webui

COPY --chown=node:node package.json yarn.lock ./
COPY --chown=node:node --from=builder /home/node/builder/dev/server/ dev/server/
COPY --chown=node:node --from=builder /home/node/builder/dev/static/ dev/static/

RUN yarn install --frozen-lockfile --production
# express is needed by the image but not by the webui npm package
RUN yarn add express@4.17.1 --ignore-scripts

EXPOSE 3000
ENTRYPOINT ["yarn", "start:dev"]
