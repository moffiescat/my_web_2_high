import request from './request'

export function getSeckillPath(goodsId) {
  return request({ url: '/seckill/path', method: 'get', params: { goodsId } })
}

export function executeSeckill(path, goodsId) {
  return request({ url: `/seckill/${path}/execute`, method: 'post', data: { goodsId } })
}

export function getSeckillResult(goodsId) {
  return request({ url: `/seckill/result/${goodsId}`, method: 'get' })
}
