-- KEYS[1]: seckill:stock:{goodsId}
-- KEYS[2]: seckill:uid:{goodsId}
-- ARGV[1]: userId

local stockKey = KEYS[1]
local uidKey = KEYS[2]
local userId = ARGV[1]

-- 检查是否已抢过
if redis.call('SISMEMBER', uidKey, userId) == 1 then
    return -1
end

-- 检查库存
local stock = tonumber(redis.call('GET', stockKey) or "0")
if stock <= 0 then
    return -2
end

-- 扣减库存 + 标记用户
redis.call('DECR', stockKey)
redis.call('SADD', uidKey, userId)
return 1
