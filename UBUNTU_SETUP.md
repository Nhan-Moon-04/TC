# Huong dan cai dat tren Ubuntu

Repo: https://github.com/Nhan-Moon-04/TC

## 1) Cai dat cong cu can thiet

```bash
sudo apt update
sudo apt install -y git
```

## 2) Cai Node.js (khuyen dung Node 18 LTS)

```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs
node -v
npm -v
```

## 3) Clone source code

```bash
git clone https://github.com/Nhan-Moon-04/TC.git
cd TC
```

## 4) Cai dat backend

```bash
cd backend
npm install
```

Tao file .env (neu chua co):

```bash
cp .env.example .env
```

Mo .env va cap nhat thong tin DB, JWT, UPLOAD_DIR neu can.

Khoi tao DB (neu dung Prisma):

```bash
npx prisma migrate deploy
npx prisma generate
```

Chay backend:

```bash
npm run dev
```

Backend mac dinh: http://localhost:3001

## 5) Cai dat frontend

Mo terminal moi:

```bash
cd TC/frontend
npm install
npm run dev
```

Frontend mac dinh: http://localhost:5173

## 6) Ghi chu

- Neu bi loi proxy /api thi kiem tra backend dang chay va dung port 3001.
- Neu can build:

```bash
cd backend
npm run build

cd ../frontend
npm run build
```
