    require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const Order = mongoose.model('Order', new mongoose.Schema({
  _id: String,               // dùng đúng id của app (ORD-...)
  trackingNumber: String, senderName: String, senderAddress: String,
  recipientName: String, recipientAddress: String,
  statusName: String, serviceType: String,
  feeVnd: Number, codVnd: Number,
  createdAt: String, estimatedDelivery: String,
  weightKg: Number, timestampMillis: Number,
}, { versionKey: false }));

const app = express();
app.use(cors(), express.json());

const toDto = o => { const { _id, ...r } = o.toObject(); return { id: _id, ...r }; };

app.get('/orders', async (_, res) =>
  res.json((await Order.find().sort({ timestampMillis: -1 })).map(toDto)));

app.post('/orders', async (req, res) => {
  const { id, ...rest } = req.body;
  await Order.findByIdAndUpdate(id, { _id: id, ...rest }, { upsert: true });
  res.sendStatus(201);
});

app.patch('/orders/:id/status', async (req, res) => {
  await Order.findByIdAndUpdate(req.params.id, { statusName: req.body.statusName });
  res.sendStatus(204);
});

app.delete('/orders/:id', async (req, res) => {
  await Order.findByIdAndDelete(req.params.id);
  res.sendStatus(204);
});

mongoose.connect(process.env.MONGODB_URI).then(() =>
  app.listen(3000, '0.0.0.0', () => console.log('API chạy cổng 3000')));