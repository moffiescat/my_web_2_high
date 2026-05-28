import request from './request'

export function getGoodsList() {
  return request({ url: '/goods/list', method: 'get' })
}

export function getGoodsDetail(id) {
  return request({ url: `/goods/detail/${id}`, method: 'get' })
}

export function getSeckillGoods() {
  return request({ url: '/goods/seckill', method: 'get' })
}
