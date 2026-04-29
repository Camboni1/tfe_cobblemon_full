FROM node:22-bookworm-slim

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install --omit=dev=false

COPY tsconfig.json ./
COPY src ./src

# Default: convert everything found under /assets/pokemon
ENV ASSETS_PATH=/assets/pokemon

ENTRYPOINT ["npx", "tsx", "src/index.ts"]
CMD ["--all"]
