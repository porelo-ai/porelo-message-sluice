# A signature as Porelo writes it

`porelo-ed25519.sig` was made with a throwaway key that exists nowhere else:

    ssh-keygen -t ed25519 -N '' -f key
    ssh-keygen -Y sign -f key -n porelo-message m.json

OpenSSH's SSHSIG, detached, Ed25519, namespace `porelo-message`, 310 bytes - the only signature Porelo
writes. The filter checks a signature's shape and never verifies it, so this one serves beside any
test message.

`old-namespace-ed25519.sig` was made the same way in the namespace the product had before it was renamed,
306 bytes; it stays to show that a signature of that shape is refused.
