# https://builtin.com/software-engineering-perspectives/ssh-port-forwarding
# ssh -R [remote_port]:[destination_address]:[local_port] [username]@[ssh_server]
ssh -R 8081:localhost:8081 \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -o ExitOnForwardFailure=yes \
    -o TCPKeepAlive=yes \
    deployer@rothbard
