local roomState = redis.call('GET', KEYS[1])
if roomState ~= 'ACTIVE' then
    return 'ROOM_NOT_ACTIVE'
end

local nonceValue = redis.call('GET', KEYS[2])
if nonceValue ~= ARGV[1] then
    return 'NONCE_INVALID'
end

redis.call('DEL', KEYS[2])
redis.call('HINCRBY', KEYS[3], ARGV[3], ARGV[2])

return 'SUCCESS'
