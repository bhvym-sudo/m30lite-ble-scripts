/* Read-only nl80211 capability query for Android arm64. No libc required.
 * Only sends CTRL_CMD_GETFAMILY and NL80211_CMD_GET_WIPHY requests.
 */
typedef unsigned short u16; typedef unsigned int u32;
static long call(long n,long a,long b,long c,long d,long e,long f) {
 register long x8 __asm__("x8")=n, x0 __asm__("x0")=a, x1 __asm__("x1")=b;
 register long x2 __asm__("x2")=c, x3 __asm__("x3")=d, x4 __asm__("x4")=e, x5 __asm__("x5")=f;
 __asm__ volatile("svc 0":"+r"(x0):"r"(x8),"r"(x1),"r"(x2),"r"(x3),"r"(x4),"r"(x5):"memory"); return x0;
}
static void out(const char *s) {long n=0;while(s[n])n++;call(64,1,(long)s,n,0,0,0);}
static void num(long n) {char b[24];int i=23;b[i]=0;do{b[--i]='0'+n%10;n/=10;}while(n);out(b+i);}
struct hdr {u32 len;u16 type,flags;u32 seq,pid;};
struct attr {u16 len,type;};
struct addr {u16 family,pad;u32 pid,groups;};
static char buf[65536];
static int request(int fd,u16 type,u16 flags,unsigned char cmd,int family) {
 char msg[40]={0}; struct hdr *h=(void*)msg;
 h->len=20;h->type=type;h->flags=flags;h->seq=family?1:2;msg[16]=cmd;msg[17]=1;
 if(family){struct attr *a=(void*)(msg+20);a->len=12;a->type=2;char *s="nl80211";for(int i=0;i<8;i++)msg[24+i]=s[i];h->len=32;}
 struct addr dst={16,0,0,0};return call(206,fd,(long)msg,h->len,0,(long)&dst,12);
}
static const char *modes[]={"unspecified","ad-hoc","managed/station","AP","AP/VLAN","WDS","monitor","mesh","P2P client","P2P GO","P2P device","OCB","NAN"};
int main(void) {
 int fd=call(198,16,3,16,0,0,0);if(fd<0){out("socket failed\n");return 1;}
 struct addr local={16,0,0,0};if(call(200,fd,(long)&local,12,0,0,0)<0){out("bind failed\n");return 1;}
 long timeout[2]={5,0};call(208,fd,1,20,(long)timeout,16,0);
 request(fd,16,1,3,1);long n=call(207,fd,(long)buf,sizeof(buf),0,0,0);int family=0;
 if(n<20){out("family lookup failed\n");return 1;}
 struct hdr *h=(void*)buf;if(h->type==2){out("family lookup returned error\n");return 1;}
 for(long p=20;p+4<=n;){struct attr *a=(void*)(buf+p);if(a->len<4||p+a->len>n)break;if(a->type==1&&a->len>=6)family=*(u16*)(buf+p+4);p+=(a->len+3)&~3;}
 if(!family){out("nl80211 not found\n");return 1;}
 out("nl80211 family: ");num(family);out("\n");request(fd,family,0x301,1,0);int found=0;
 for(;;){n=call(207,fd,(long)buf,sizeof(buf),0,0,0);if(n<0){out("receive failed or timed out\n");return 1;}
 for(long p=0;p+16<=n;){h=(void*)(buf+p);if(h->len<16||p+h->len>n)return 1;
 if(h->type==3)return found?0:1;
 if(h->type==2){out("kernel error\n");return 1;}
 for(long q=p+20;q+4<=p+h->len;){struct attr *a=(void*)(buf+q);if(a->len<4||q+a->len>p+h->len)break;int t=a->type&0x3fff;
 if(t==2){out("PHY: ");for(int j=4;j<a->len&&buf[q+j];j++)call(64,1,(long)(buf+q+j),1,0,0,0);out("\n");}
 if(t==32){found=1;out("Supported interface modes:\n");for(long r=q+4;r+4<=q+a->len;){struct attr *b=(void*)(buf+r);if(b->len<4||r+b->len>q+a->len)break;int m=b->type&0x3fff;out("  ");if(m<13)out(modes[m]);else num(m);out("\n");r+=(b->len+3)&~3;}}
 q+=(a->len+3)&~3;}
 p+=(h->len+3)&~3;}}
}
__asm__(".global _start\n_start:\nbl main\nmov x8, #93\nsvc #0\n");
