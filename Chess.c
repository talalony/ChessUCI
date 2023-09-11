#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>


#ifdef WIN64
    #include <windows.h>
#else
    #include <sys/time.h>
#endif

#include "nnue_eval.h"


#define U64 unsigned long long


#define empty_board "8/8/8/8/8/8/8/8 w - -"
#define start_position "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
#define tricky_position "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
#define killer_position "rnbqkb1r/pp1p1pPp/8/2p1pP2/1P1P4/3P3P/P1P1P3/RNBQKBNR w KQkq e6 0 1"
#define cmk_position "r2q1rk1/ppp2ppp/2n1bn2/2b1p3/3pP3/3P1NPP/PPP1NPB1/R1BQ1RK1 b - - 0 9"
#define endgame_position "8/k7/3p4/p2P1p2/P2P1P2/8/8/K7 w - - 0 8"
#define repetition "2r3k1/R7/8/1R6/8/8/P4KPP/8 w - - 17 1 "

//           bit manipulations
// ====================================
#define GET_BIT(bitboard, square) ((bitboard) & (1ULL << (square)))
#define SET_BIT(bitboard, square) ((bitboard) |= (1ULL << (square)))
#define RESET_BIT(bitboard, square) ((bitboard) &= ~(1ULL << (square)))


/*
           binary move bits              data            hexidecimal constants

    0000 0000 0000 0000 0011 1111    source square       0x3f
    0000 0000 0000 1111 1100 0000    target square       0xfc0
    0000 0000 1111 0000 0000 0000    piece               0xf000
    0000 1111 0000 0000 0000 0000    promoted piece      0xf0000
    0001 0000 0000 0000 0000 0000    capture flag        0x100000
    0010 0000 0000 0000 0000 0000    double push flag    0x200000
    0100 0000 0000 0000 0000 0000    enpassant flag      0x400000
    1000 0000 0000 0000 0000 0000    castling flag       0x800000
*/
#define ENCODE_MOVE(source, target, piece, promoted, capture, double, enpassant, castling) \
    (source) | ((target) << 6) | ((piece) << 12) | ((promoted) << 16) | ((capture) << 20) | ((double) << 21) | ((enpassant) << 22) | ((castling) << 23)

#define GET_MOVE_SOURCE(move) ((move) & 0x3f)
#define GET_MOVE_TARGET(move) (((move) & 0xfc0) >> 6)
#define GET_MOVE_PIECE(move) (((move) & 0xf000) >> 12)
#define GET_MOVE_PROMOTED(move) (((move) & 0xf0000) >> 16)
#define GET_MOVE_CAPTURE(move) ((move) & 0x100000)
#define GET_MOVE_DOUBLE(move) ((move) & 0x200000)
#define GET_MOVE_ENPASSANT(move) ((move) & 0x400000)
#define GET_MOVE_CASTLING(move) ((move) & 0x800000)

const U64 k1 = 0x5555555555555555; /*  -1/3   */
const U64 k2 = 0x3333333333333333; /*  -1/5   */
const U64 k4 = 0x0f0f0f0f0f0f0f0f; /*  -1/17  */
const U64 kf = 0x0101010101010101; /*  -1/255 */

static inline int countOnes (U64 x) {
    x =  x       - ((x >> 1)  & k1); /* put count of each 2 bits into those 2 bits */
    x = (x & k2) + ((x >> 2)  & k2); /* put count of each 4 bits into those 4 bits */
    x = (x       +  (x >> 4)) & k4 ; /* put count of each 8 bits into those 8 bits */
    x = (x * kf) >> 56; /* returns 8 most significant bits of x + (x<<8) + (x<<16) + (x<<24) + ...  */
    return (int) x;
}

const int index64[64] = {
    0, 1, 17, 2, 18, 50, 3, 57,
    47, 19, 22, 51, 29, 4, 33, 58,
    15, 48, 20, 27, 25, 23, 52, 41,
    54, 30, 38, 5, 43, 34, 59, 8,
    63, 16, 49, 56, 46, 21, 28, 32,
    14, 26, 24, 40, 53, 37, 42, 7,
    62, 55, 45, 31, 13, 39, 36, 6,
    61, 44, 12, 35, 60, 11, 10, 9,
};
const U64 Magic = 0x37E84A99DAE458F;

static inline int getIdxOfLSB(U64 b)
{
    return index64[((b & -b)*Magic) >> 58];
}

typedef struct 
{
    int moves[256];
    int counter;

} moves;

static inline void addMove(moves *moveList, int move) 
{
    moveList->moves[moveList->counter] = move;
    moveList->counter++;
}

//              decleration
// ====================================
static inline int isSquareAttacked(int square, int turn);


//           random numbers
// ====================================

unsigned int randomState = 1804289383;

unsigned int getRandomU32Number()
{
    unsigned int x = randomState;
    x ^= x << 13;
    x ^= x >> 17;
    x ^= x << 5;

    randomState = x;

    return x;
}

U64 getRandomU64Number()
{
    U64 n1, n2, n3, n4;

    n1 = (U64)(getRandomU32Number()) & 0xFFFF;
    n2 = (U64)(getRandomU32Number()) & 0xFFFF;
    n3 = (U64)(getRandomU32Number()) & 0xFFFF;
    n4 = (U64)(getRandomU32Number()) & 0xFFFF;

    return n1 | (n2 << 16) | (n3 << 32) | (n4 << 48);
}

U64 generateMagicNumber()
{
    return getRandomU64Number() & getRandomU64Number() & getRandomU64Number();
}

enum 
{
    a8, b8, c8, d8, e8, f8, g8, h8,
    a7, b7, c7, d7, e7, f7, g7, h7,
    a6, b6, c6, d6, e6, f6, g6, h6,
    a5, b5, c5, d5, e5, f5, g5, h5,
    a4, b4, c4, d4, e4, f4, g4, h4,
    a3, b3, c3, d3, e3, f3, g3, h3,
    a2, b2, c2, d2, e2, f2, g2, h2,
    a1, b1, c1, d1, e1, f1, g1, h1, noSq
};

enum { white, black, both };

enum { rook, bishop };

enum { wk = 1, wq = 2, bk = 4, bq = 8 };

enum { P, N, B, R, Q, K, p, n, b, r, q, k };

const char *squareToCoord[] = {
"a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
"a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
"a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
"a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
"a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
"a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
"a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
"a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1"
};

char asciiPieces[12] = "PNBRQKpnbrqk";

char *unicodePieces[12] = {"♟︎","♞","♝","♜","♛","♚","♙","♘","♗","♖","♕","♔"};

int charPieces[] = {
['P'] = P,
['N'] = N,
['B'] = B,
['R'] = R,
['Q'] = Q,
['K'] = K,
['p'] = p,
['n'] = n,
['b'] = b,
['r'] = r,
['q'] = q,
['k'] = k
};

char promotedPieces[] = {
    [Q] = 'q',
    [R] = 'r',
    [N] = 'n',
    [B] = 'b',
    [q] = 'q',
    [r] = 'r',
    [n] = 'n',
    [b] = 'b'
};

//              bitboards
// ====================================

U64 bitboards[12];
U64 occupancies[3];

int turn = -1;

int enpassant = noSq;

int castle;

U64 hashKey;

U64 repetitionTable[1000];
int repetitionIndex;

int ply;

int fifty;

//               Zobrist
// ====================================

U64 pieceKeys[12][64];

U64 enpassantKeys[64];

U64 castleKeys[16];

U64 turnKey;

void initRandomKeys()
{
    randomState = 1804289383;
    for (int piece = P; piece <= k; piece++)
        for (int square = 0; square < 64; square++)
            pieceKeys[piece][square] = getRandomU64Number();

    for (int square = 0; square < 64; square++)
        enpassantKeys[square] = getRandomU64Number();

    for (int i = 0; i < 16; i++)
        castleKeys[i] = getRandomU64Number();

    turnKey = getRandomU64Number();
}

U64 generateHashKey()
{
    U64 finalKey = 0ULL;

    U64 bitboard;
    for (int piece = P; piece <= k; piece++)
    {
        bitboard = bitboards[piece];
        while (bitboard)
        {
            int square = getIdxOfLSB(bitboard);

            finalKey ^= pieceKeys[piece][square];

            RESET_BIT(bitboard, square);
        }
    }

    if (enpassant != noSq) finalKey ^= enpassantKeys[enpassant];
    
    finalKey ^= castleKeys[castle];

    if (turn == black) finalKey ^= turnKey;

    return finalKey;
}

//           time control
// ====================================

// exit from engine flag
int quit = 0;

// UCI "movestogo" command moves counter
int movestogo = 30;

// UCI "movetime" command time counter
int movetime = -1;

// UCI "time" command holder (ms)
int time = -1;

// UCI "inc" command's time increment holder
int inc = 0;

// UCI "starttime" command time holder
int starttime = 0;

// UCI "stoptime" command time holder
int stoptime = 0;

// variable to flag time control availability
int timeset = 0;

// variable to flag when the time is up
int stopped = 0;

int getTimeInMillis()
{
    #ifdef WIN64
        return GetTickCount();
    #else
        struct timeval timeval;
        gettimeofday(timeval, NULL);
        return timeval.tv_sec * 1000 + timeval.tv_usec / 1000;
    #endif
}

 // forked from VICE by BluefeverSoftware
int input_waiting()
{
    #ifndef WIN32
        fd_set readfds;
        struct timeval tv;
        FD_ZERO (&readfds);
        FD_SET (fileno(stdin), &readfds);
        tv.tv_sec=0; tv.tv_usec=0;
        select(16, &readfds, 0, 0, &tv);

        return (FD_ISSET(fileno(stdin), &readfds));
    #else
        static int init = 0, pipe;
        static HANDLE inh;
        DWORD dw;

        if (!init)
        {
            init = 1;
            inh = GetStdHandle(STD_INPUT_HANDLE);
            pipe = !GetConsoleMode(inh, &dw);
            if (!pipe)
            {
                SetConsoleMode(inh, dw & ~(ENABLE_MOUSE_INPUT|ENABLE_WINDOW_INPUT));
                FlushConsoleInputBuffer(inh);
            }
        }
        
        if (pipe)
        {
           if (!PeekNamedPipe(inh, NULL, 0, NULL, &dw, NULL)) return 1;
           return dw;
        }
        
        else
        {
           GetNumberOfConsoleInputEvents(inh, &dw);
           return dw <= 1 ? 0 : dw;
        }

    #endif
}

// read GUI/user input
void read_input()
{
    // bytes to read holder
    int bytes;
    
    // GUI/user input
    char input[256] = "", *endc;

    // "listen" to STDIN
    if (input_waiting())
    {
        // tell engine to stop calculating
        stopped = 1;
        
        // loop to read bytes from STDIN
        do
        {
            // read bytes from STDIN
            bytes=read(fileno(stdin), input, 256);
        }
        
        // until bytes available
        while (bytes < 0);
        
        // searches for the first occurrence of '\n'
        endc = strchr(input,'\n');
        
        // if found new line set value at pointer to 0
        if (endc) *endc=0;
        
        // if input is available
        if (strlen(input) > 0)
        {
            // match UCI "quit" command
            if (!strncmp(input, "quit", 4))
            {
                // tell engine to terminate exacution    
                quit = 1;
            }

            // // match UCI "stop" command
            else if (!strncmp(input, "stop", 4))    {
                // tell engine to terminate exacution
                quit = 1;
            }
        }   
    }
}

// a bridge function to interact between search and GUI input
static void communicate() {
	// if time is up break here
    if(timeset == 1 && getTimeInMillis() > stoptime) {
		// tell engine to stop calculating
		stopped = 1;
	}
	
    // read GUI input
	read_input();
}

//               prints
// ====================================

void printBitboard(U64 bitboard)
{
    for (int rank = 0; rank < 8; rank++)
    {
        for (int file = 0; file < 8; file++)
        {
            int square = rank * 8 + file;

            if(!file)
                printf("  %d  ", 8-rank);

            printf(" %d ", GET_BIT(bitboard, square) ? 1 : 0);
        }
        printf("\n");
    }

    printf("\n      a  b  c  d  e  f  g  h\n\n");
    printf("      Bitboard:  %llud\n\n", bitboard);

}

void printBoard()
{
    printf("\n");
    for (int rank = 0; rank < 8; rank++)
    {
        for (int file = 0; file < 8; file++)
        {
            int square = rank * 8 + file;
            if (!file)
                printf("  %d  ", 8-rank);

            int piece = -1;

            for (int bbPiece = P; bbPiece <= k; bbPiece++)
            {
                if (GET_BIT(bitboards[bbPiece], square))
                    piece = bbPiece;
            }
            printf(" %c ", (piece == -1 ? '.' : asciiPieces[piece]));
        }
        printf("\n");
    }
    printf("\n      a  b  c  d  e  f  g  h\n\n");

    printf("         Turn:    %s\n", !turn ? "white" : "black");

    printf("         Enpassant:  %s\n", (enpassant != noSq) ? squareToCoord[enpassant] : "no");

    printf("         Castling: %c%c%c%c\n\n", (castle & wk ) ? 'K' : '-', (castle & wq ) ? 'Q' : '-', (castle & bk ) ? 'k' : '-', (castle & bq ) ? 'q' : '-');

    printf("         Hash key: %llx\n\n", hashKey);

    printf("         Fifty move: %d\n\n", fifty);

}

void resetBoard()
{
    // reset board position (bitboards)
    memset(bitboards, 0ULL, sizeof(bitboards));
    
    // reset occupancies (bitboards)
    memset(occupancies, 0ULL, sizeof(occupancies));
    
    // reset game state variables
    turn = 0;
    enpassant = noSq;
    castle = 0;
    
    // reset repetition index
    repetitionIndex = 0;
    
    // reset fifty move rule counter
    fifty = 0;
    
    // reset repetition table
    memset(repetitionTable, 0ULL, sizeof(repetitionTable));
}

void printAttackedSquares(int turn)
{
    printf("\n");
    for (int rank = 0; rank < 8; rank++)
    {
        for (int file = 0; file < 8; file++)
        {
            int square = rank*8+file;
            if (!file)
                printf("  %d  ", 8-rank);
            printf(" %d ", isSquareAttacked(square, turn) ? 1 : 0);
        }
        printf("\n");
    }
    printf("\n      a  b  c  d  e  f  g  h\n\n");
}


void parseFEN(char *fen)
{

    resetBoard();

    for (int rank = 0; rank < 8; rank++)
    {
        for (int file = 0; file < 8; file++)
        {
            int square = rank * 8 + file;
            if (*fen >= 'a' && *fen <= 'z' || *fen >= 'A' && *fen <= 'Z')
            {
                int piece = charPieces[*fen];
                SET_BIT(bitboards[piece], square);
                fen++;
            }

            if (*fen >= '0' && *fen <= '9')
            {
                // convert from char char '0' to number 0
                int offset = *fen - '0';
                int piece = -1;

                for (int bbPiece = P; bbPiece <= k; bbPiece++)
                {
                    if (GET_BIT(bitboards[bbPiece], square))
                        piece = bbPiece;
                }

                if (piece == -1)
                    file--;
                file += offset;
                fen++;
            }

            if (*fen == '/')
            {
                fen++;
            }
        }
    }
    fen++;
    turn = (*fen == 'w') ? white : black;
    fen+=2;

    while (*fen != ' ')
    {
        switch (*fen)
        {
        case 'K':
            castle |= wk;
            break;
        case 'Q':
            castle |= wq;
            break;
        case 'k':
            castle |= bk;
            break;
        case 'q':
            castle |= bq;
            break;
        case '-':
            break;
        }
        fen++;
    }
    fen++;
    if (*fen != '-')
    {
        int file = fen[0] - 'a';
        int rank = 8 - (fen[1] - '0');

        enpassant = rank*8+file;
    }
    else
        enpassant = noSq;
    
    for (int piece = P; piece <= K; piece++)
        occupancies[white] |= bitboards[piece];
    for (int piece = p; piece <= k; piece++)
        occupancies[black] |= bitboards[piece];
    occupancies[both] |= occupancies[white];
    occupancies[both] |= occupancies[black];
    hashKey = generateHashKey();
}

// for UCI
void printMove(int move)
{
    if (GET_MOVE_PROMOTED(move))
        printf("%s%s%c", squareToCoord[GET_MOVE_SOURCE(move)], squareToCoord[GET_MOVE_TARGET(move)], promotedPieces[GET_MOVE_PROMOTED(move)]);
    else
        printf("%s%s", squareToCoord[GET_MOVE_SOURCE(move)], squareToCoord[GET_MOVE_TARGET(move)]);
}

void printMoveList(moves * moveList)
{
    if (!moveList->counter) 
    {
        printf("\nMove list is empty!\n");
        return;
    }
    printf("\n   move    piece   capture   double   enpassant   castling\n\n");
    for (int moveCount = 0; moveCount < moveList->counter; moveCount++)
    {
        int move = moveList->moves[moveCount];
        printf("   %s%s%c     %c        %d         %d         %d          %d\n", squareToCoord[GET_MOVE_SOURCE(move)], squareToCoord[GET_MOVE_TARGET(move)],
                                                                                 promotedPieces[GET_MOVE_PROMOTED(move)] ? promotedPieces[GET_MOVE_PROMOTED(move)] : ' ',
                                                                                 asciiPieces[GET_MOVE_PIECE(move)], GET_MOVE_CAPTURE(move) ? 1 : 0, GET_MOVE_DOUBLE(move) ? 1 : 0,
                                                                                 GET_MOVE_ENPASSANT(move) ? 1 : 0, GET_MOVE_CASTLING(move) ? 1 : 0);
    }
    printf("\n   Total number of moves: %d\n\n", moveList->counter);
}

//               Attacks
// ====================================

const U64 notAFile = 18374403900871474942ULL;
const U64 notHFile = 9187201950435737471ULL;
const U64 notABFile = 18229723555195321596ULL;
const U64 notGHFile = 4557430888798830399ULL;


const int bishopRelevantBits[64] = { 
    6,  5,  5,  5,  5,  5,  5,  6, 
    5,  5,  5,  5,  5,  5,  5,  5,
    5,  5,  7,  7,  7,  7,  5,  5,
    5,  5,  7,  9,  9,  7,  5,  5,
    5,  5,  7,  9,  9,  7,  5,  5,
    5,  5,  7,  7,  7,  7,  5,  5,
    5,  5,  5,  5,  5,  5,  5,  5,
    6,  5,  5,  5,  5,  5,  5,  6
    };

const int rookRelevantBits[64] = {
    12,  11,  11,  11,  11,  11,  11,  12, 
    11,  10,  10,  10,  10,  10,  10,  11,
    11,  10,  10,  10,  10,  10,  10,  11,
    11,  10,  10,  10,  10,  10,  10,  11,
    11,  10,  10,  10,  10,  10,  10,  11,
    11,  10,  10,  10,  10,  10,  10,  11,
    11,  10,  10,  10,  10,  10,  10,  11,
    12,  11,  11,  11,  11,  11,  11,  12
};

U64 rookMagicNumbers[64] = {
    0x8a80104000800020ULL,
    0x140002000100040ULL,
    0x2801880a0017001ULL,
    0x100081001000420ULL,
    0x200020010080420ULL,
    0x3001c0002010008ULL,
    0x8480008002000100ULL,
    0x2080088004402900ULL,
    0x800098204000ULL,
    0x2024401000200040ULL,
    0x100802000801000ULL,
    0x120800800801000ULL,
    0x208808088000400ULL,
    0x2802200800400ULL,
    0x2200800100020080ULL,
    0x801000060821100ULL,
    0x80044006422000ULL,
    0x100808020004000ULL,
    0x12108a0010204200ULL,
    0x140848010000802ULL,
    0x481828014002800ULL,
    0x8094004002004100ULL,
    0x4010040010010802ULL,
    0x20008806104ULL,
    0x100400080208000ULL,
    0x2040002120081000ULL,
    0x21200680100081ULL,
    0x20100080080080ULL,
    0x2000a00200410ULL,
    0x20080800400ULL,
    0x80088400100102ULL,
    0x80004600042881ULL,
    0x4040008040800020ULL,
    0x440003000200801ULL,
    0x4200011004500ULL,
    0x188020010100100ULL,
    0x14800401802800ULL,
    0x2080040080800200ULL,
    0x124080204001001ULL,
    0x200046502000484ULL,
    0x480400080088020ULL,
    0x1000422010034000ULL,
    0x30200100110040ULL,
    0x100021010009ULL,
    0x2002080100110004ULL,
    0x202008004008002ULL,
    0x20020004010100ULL,
    0x2048440040820001ULL,
    0x101002200408200ULL,
    0x40802000401080ULL,
    0x4008142004410100ULL,
    0x2060820c0120200ULL,
    0x1001004080100ULL,
    0x20c020080040080ULL,
    0x2935610830022400ULL,
    0x44440041009200ULL,
    0x280001040802101ULL,
    0x2100190040002085ULL,
    0x80c0084100102001ULL,
    0x4024081001000421ULL,
    0x20030a0244872ULL,
    0x12001008414402ULL,
    0x2006104900a0804ULL,
    0x1004081002402ULL,
};

U64 bishopMagicNumbers[64] = {
    0x40040844404084ULL,
    0x2004208a004208ULL,
    0x10190041080202ULL,
    0x108060845042010ULL,
    0x581104180800210ULL,
    0x2112080446200010ULL,
    0x1080820820060210ULL,
    0x3c0808410220200ULL,
    0x4050404440404ULL,
    0x21001420088ULL,
    0x24d0080801082102ULL,
    0x1020a0a020400ULL,
    0x40308200402ULL,
    0x4011002100800ULL,
    0x401484104104005ULL,
    0x801010402020200ULL,
    0x400210c3880100ULL,
    0x404022024108200ULL,
    0x810018200204102ULL,
    0x4002801a02003ULL,
    0x85040820080400ULL,
    0x810102c808880400ULL,
    0xe900410884800ULL,
    0x8002020480840102ULL,
    0x220200865090201ULL,
    0x2010100a02021202ULL,
    0x152048408022401ULL,
    0x20080002081110ULL,
    0x4001001021004000ULL,
    0x800040400a011002ULL,
    0xe4004081011002ULL,
    0x1c004001012080ULL,
    0x8004200962a00220ULL,
    0x8422100208500202ULL,
    0x2000402200300c08ULL,
    0x8646020080080080ULL,
    0x80020a0200100808ULL,
    0x2010004880111000ULL,
    0x623000a080011400ULL,
    0x42008c0340209202ULL,
    0x209188240001000ULL,
    0x400408a884001800ULL,
    0x110400a6080400ULL,
    0x1840060a44020800ULL,
    0x90080104000041ULL,
    0x201011000808101ULL,
    0x1a2208080504f080ULL,
    0x8012020600211212ULL,
    0x500861011240000ULL,
    0x180806108200800ULL,
    0x4000020e01040044ULL,
    0x300000261044000aULL,
    0x802241102020002ULL,
    0x20906061210001ULL,
    0x5a84841004010310ULL,
    0x4010801011c04ULL,
    0xa010109502200ULL,
    0x4a02012000ULL,
    0x500201010098b028ULL,
    0x8040002811040900ULL,
    0x28000010020204ULL,
    0x6000020202d0240ULL,
    0x8918844842082200ULL,
    0x4010011029020020ULL,
};



U64 pawnAttacks[2][64];
U64 knightAttacks[64];
U64 kingAttacks[64];
U64 whiteKingZone[64];
U64 blackKingZone[64];


U64 bishopMasks[64];
U64 rookMasks[64];

U64 bishopAttacks[64][512];
U64 rookAttacks[64][4096];

U64 maskPawnAttacks(int turn, int square)
{
    U64 attacks = 0ULL;
    U64 bitboard = 0ULL;

    SET_BIT(bitboard, square);
    if (!turn) // white
    {
        if ((bitboard >> 7) & notAFile) attacks |= (bitboard >> 7);
        if ((bitboard >> 9) & notHFile) attacks |= (bitboard >> 9);
    }
    else // black
    {
        if ((bitboard << 7) & notHFile) attacks |= (bitboard << 7);
        if ((bitboard << 9) & notAFile) attacks |= (bitboard << 9);
    }

    return attacks;

}

U64 maskKnightAttacks(int square)
{
    U64 attacks = 0ULL;
    U64 bitboard = 0ULL;

    SET_BIT(bitboard, square);
    if ((bitboard >> 17) & notHFile) attacks |= (bitboard >> 17);
    if ((bitboard >> 15) & notAFile) attacks |= (bitboard >> 15);
    if ((bitboard >> 10) & notGHFile) attacks |= (bitboard >> 10);
    if ((bitboard >> 6) & notABFile) attacks |= (bitboard >> 6);

    if ((bitboard << 17) & notAFile) attacks |= (bitboard << 17);
    if ((bitboard << 15) & notHFile) attacks |= (bitboard << 15);
    if ((bitboard << 10) & notABFile) attacks |= (bitboard << 10);
    if ((bitboard << 6) & notGHFile) attacks |= (bitboard << 6);

    return attacks;

}

U64 maskKingAttacks(int square)
{
    U64 attacks = 0ULL;
    U64 bitboard = 0ULL;

    SET_BIT(bitboard, square);
    if ((bitboard >> 8)) attacks |= (bitboard >> 8);
    if ((bitboard >> 1) & notHFile) attacks |= (bitboard >> 1);
    if ((bitboard >> 9) & notHFile) attacks |= (bitboard >> 9);
    if ((bitboard >> 7) & notAFile) attacks |= (bitboard >> 7);

    if ((bitboard << 8)) attacks |= (bitboard << 8);
    if ((bitboard << 1) & notAFile) attacks |= (bitboard << 1);
    if ((bitboard << 9) & notAFile) attacks |= (bitboard << 9);
    if ((bitboard << 7) & notHFile) attacks |= (bitboard << 7);

    return attacks;
}

U64 maskKingZone(int square, int color)
{
    U64 attacks = 0ULL;
    U64 bitboard = 0ULL;

    SET_BIT(bitboard, square);
    if ((bitboard >> 8)) attacks |= (bitboard >> 8);
    if ((bitboard >> 1) & notHFile) attacks |= (bitboard >> 1);
    if ((bitboard >> 9) & notHFile) attacks |= (bitboard >> 9);
    if ((bitboard >> 7) & notAFile) attacks |= (bitboard >> 7);

    if ((bitboard << 8)) attacks |= (bitboard << 8);
    if ((bitboard << 1) & notAFile) attacks |= (bitboard << 1);
    if ((bitboard << 9) & notAFile) attacks |= (bitboard << 9);
    if ((bitboard << 7) & notHFile) attacks |= (bitboard << 7);

    if (color == white)
    {
        if ((bitboard >> 16)) attacks |= (bitboard >> 16);
        if ((bitboard >> 15) & notAFile) attacks |= (bitboard >> 15);
        if ((bitboard >> 17) & notHFile) attacks |= (bitboard >> 17);
    }
    else
    {
        if ((bitboard << 16)) attacks |= (bitboard << 16);
        if ((bitboard << 15) & notHFile) attacks |= (bitboard << 15);
        if ((bitboard << 17) & notAFile) attacks |= (bitboard << 17);
    }
    // if ((bitboard >> 2) & notGHFile) attacks |= (bitboard >> 2);
    // if ((bitboard >> 18) & notGHFile) attacks |= (bitboard >> 18);
    // if ((bitboard >> 14) & notABFile) attacks |= (bitboard >> 14);

    // if ((bitboard >> 6) & notABFile) attacks |= (bitboard >> 6);
    // if ((bitboard >> 10) & notGHFile) attacks |= (bitboard >> 10);

    // if ((bitboard << 2) & notABFile) attacks |= (bitboard << 2);
    // if ((bitboard << 18) & notABFile) attacks |= (bitboard << 18);
    // if ((bitboard << 14) & notGHFile) attacks |= (bitboard << 14);

    // if ((bitboard << 6) & notGHFile) attacks |= (bitboard << 6);
    // if ((bitboard << 10) & notABFile) attacks |= (bitboard << 10);

    return attacks;
}

void initLeapersAttacks()
{
    for (int square = 0; square < 64; square++)
    {
        pawnAttacks[white][square] = maskPawnAttacks(white, square);
        pawnAttacks[black][square] = maskPawnAttacks(black, square);
        knightAttacks[square] = maskKnightAttacks(square);
        kingAttacks[square] = maskKingAttacks(square);
        whiteKingZone[square] = maskKingZone(square, white);
        blackKingZone[square] = maskKingZone(square, black);


    }
}

U64 maskBishopAttacks(int square)
{
    U64 attacks = 0ULL;

    int r, f;
    int tr = square / 8;
    int tf = square % 8;

    for (r = tr+1, f = tf+1; r <= 6 && f <= 6; r++, f++) attacks |= (1ULL << (r*8+f));
    for (r = tr-1, f = tf+1; r >= 1 && f <= 6; r--, f++) attacks |= (1ULL << (r*8+f));
    for (r = tr+1, f = tf-1; r <= 6 && f >= 1; r++, f--) attacks |= (1ULL << (r*8+f));
    for (r = tr-1, f = tf-1; r >=1 && f >=1; r--, f--) attacks |= (1ULL << (r*8+f));

    return attacks;
}

U64 bishopAttacksInRunTime(int square, U64 block)
{
    U64 attacks = 0ULL;

    int r, f;
    int tr = square / 8;
    int tf = square % 8;

    for (r = tr+1, f = tf+1; r <= 7 && f <= 7; r++, f++)
    {
        attacks |= (1ULL << (r*8+f));
        if ((1ULL << (r*8+f)) & block) break;
    }
    for (r = tr-1, f = tf+1; r >= 0 && f <= 7; r--, f++)
    {
        attacks |= (1ULL << (r*8+f));
        if ((1ULL << (r*8+f)) & block) break;
    }
    for (r = tr+1, f = tf-1; r <= 7 && f >= 0; r++, f--) 
    {
        attacks |= (1ULL << (r*8+f));
        if ((1ULL << (r*8+f)) & block) break;
    }
    for (r = tr-1, f = tf-1; r >= 0 && f >= 0; r--, f--) 
    {
        attacks |= (1ULL << (r*8+f));
        if ((1ULL << (r*8+f)) & block) break;
    }

    return attacks;
}

U64 maskRookAttacks(int square)
{
    U64 attacks = 0ULL;

    int r, f;
    int tr = square / 8;
    int tf = square % 8;

    for (r = tr+1; r <= 6; r++) attacks |= (1ULL << (r*8+tf));
    for (r = tr-1; r >= 1; r--) attacks |= (1ULL << (r*8+tf));
    for (f = tf+1; f <= 6; f++) attacks |= (1ULL << (tr*8+f));
    for (f = tf-1; f >= 1; f--) attacks |= (1ULL << (tr*8+f));

    return attacks;
}

U64 rookAttacksInRunTime(int square, U64 block)
{
    U64 attacks = 0ULL;

    int r, f;
    int tr = square / 8;
    int tf = square % 8;

    for (r = tr+1; r <= 7; r++) 
    {
        attacks |= (1ULL << (r*8+tf));
        if ((1ULL << (r*8+tf)) & block) break;
    }
    for (r = tr-1; r >= 0; r--) 
    {
        attacks |= (1ULL << (r*8+tf));
        if ((1ULL << (r*8+tf)) & block) break;
    }
    for (f = tf+1; f <= 7; f++) 
    {
        attacks |= (1ULL << (tr*8+f));
        if ((1ULL << (tr*8+f)) & block) break;

    }
    for (f = tf-1; f >= 0; f--) 
    {
        attacks |= (1ULL << (tr*8+f));
        if ((1ULL << (tr*8+f)) & block) break;
    }

    return attacks;
}

U64 setOccupancy(int index, int bitsInMask, U64 attackMask)
{
    U64 occupancy = 0Ull;

    for (int count = 0; count < bitsInMask; count++)
    {
        int square = getIdxOfLSB(attackMask);
        RESET_BIT(attackMask, square);
        if (index & (1 << count))
            occupancy |= (1Ull << square);
    }

    return occupancy;
}

void initSlidersAttacks(int bishop) 
{
    for (int square = 0; square < 64; square++)
    {
        bishopMasks[square] = maskBishopAttacks(square);
        rookMasks[square] = maskRookAttacks(square);

        U64 attackMask = bishop ? bishopMasks[square] : rookMasks[square];

        int relevantBits = countOnes(attackMask);
        int occupancyIndicies = (1 << relevantBits);

        for (int idx = 0; idx < occupancyIndicies; idx++)
        {
            if (bishop)
            {
                U64 occupancy = setOccupancy(idx, relevantBits, attackMask);
                int magicIdx = (occupancy * bishopMagicNumbers[square]) >> (64-bishopRelevantBits[square]); 
                bishopAttacks[square][magicIdx] = bishopAttacksInRunTime(square, occupancy);
            }
            else
            {
                U64 occupancy = setOccupancy(idx, relevantBits, attackMask);
                int magicIdx = (occupancy * rookMagicNumbers[square]) >> (64-rookRelevantBits[square]); 
                rookAttacks[square][magicIdx] = rookAttacksInRunTime(square, occupancy);
            }
        }
    }
}

static inline U64 getBishopAttacks(int square, U64 occupancy)
{
    occupancy &= bishopMasks[square];
    occupancy *= bishopMagicNumbers[square];
    occupancy >>= 64 - bishopRelevantBits[square];

    return bishopAttacks[square][occupancy];
}

static inline U64 getRookAttacks(int square, U64 occupancy)
{
    occupancy &= rookMasks[square];
    occupancy *= rookMagicNumbers[square];
    occupancy >>= 64 - rookRelevantBits[square];

    return rookAttacks[square][occupancy];

}

static inline U64 getQueenAttacks(int square, U64 occupancy)
{
    U64 queenAttacks = 0ULL;

    U64 bishopOccupancy = occupancy;
    U64 rookOccupancy = occupancy;


    bishopOccupancy &= bishopMasks[square];
    bishopOccupancy *= bishopMagicNumbers[square];
    bishopOccupancy >>= 64 - bishopRelevantBits[square];

    queenAttacks = bishopAttacks[square][bishopOccupancy];

    rookOccupancy &= rookMasks[square];
    rookOccupancy *= rookMagicNumbers[square];
    rookOccupancy >>= 64 - rookRelevantBits[square];

    queenAttacks |= rookAttacks[square][rookOccupancy];

    return queenAttacks;

}

static inline int isSquareAttacked(int square, int turn)
{
    // pawns
    if ((turn == white) && (pawnAttacks[black][square] & bitboards[P])) return 1;
    if ((turn == black) && (pawnAttacks[white][square] & bitboards[p])) return 1;

    // knights
    if (knightAttacks[square] & ((turn == white) ?  bitboards[N] : bitboards[n])) return 1;

    // kings
    if (kingAttacks[square] & ((turn == white) ?  bitboards[K] : bitboards[k])) return 1;


    // bishops
    if (getBishopAttacks(square, occupancies[both]) & ((turn == white) ?  bitboards[B] : bitboards[b])) return 1;

    // rook
    if (getRookAttacks(square, occupancies[both]) & ((turn == white) ?  bitboards[R] : bitboards[r])) return 1;

    // queen
    if (getQueenAttacks(square, occupancies[both]) & ((turn == white) ?  bitboards[Q] : bitboards[q])) return 1;

    return 0;
}

//          Move generation
// ====================================

#define COPY_BOARD()                                  \
        U64 bitboardsCopy[12], occupanciesCopy[3];    \
        int turnCopy, enpassantCopy, castleCopy, fiftyCopy;      \
        memcpy(bitboardsCopy, bitboards, 96);        \
        memcpy(occupanciesCopy, occupancies, 24);     \
        turnCopy = turn, enpassantCopy = enpassant, castleCopy = castle; \
        fiftyCopy = fifty; \
        U64 hashKeyCopy = hashKey;

#define RESTORE_BOARD()                           \
        memcpy(bitboards, bitboardsCopy, 96);    \
        memcpy(occupancies, occupanciesCopy, 24); \
        turn = turnCopy, enpassant = enpassantCopy, castle = castleCopy; \
        fifty = fiftyCopy; \
        hashKey = hashKeyCopy;

enum { allMoves, onlyCaptures };

const int castlingRights[64] = {
    7, 15, 15, 15, 3, 15, 15, 11,
    15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15,
    13, 15, 15, 15, 12, 15, 15, 14
};


static inline int makeMove(int move, int move_flag)
{
    // quiet moves
    if (move_flag == allMoves)
    {
        // preserve board state
        COPY_BOARD();
        
        // parse move
        int source_square = GET_MOVE_SOURCE(move);
        int target_square = GET_MOVE_TARGET(move);
        int piece = GET_MOVE_PIECE(move);
        int promoted_piece = GET_MOVE_PROMOTED(move);
        int capture = GET_MOVE_CAPTURE(move);
        int double_push = GET_MOVE_DOUBLE(move);
        int enpass = GET_MOVE_ENPASSANT(move);
        int castling = GET_MOVE_CASTLING(move);
        
        // move piece
        RESET_BIT(bitboards[piece], source_square);
        SET_BIT(bitboards[piece], target_square);

        hashKey ^= pieceKeys[piece][source_square];
        hashKey ^= pieceKeys[piece][target_square];

        fifty++;

        if (piece == P || piece == p) fifty = 0;
        // handling capture moves
        if (capture)
        {
            fifty = 0;
            // pick up bitboard piece index ranges depending on side
            int start_piece, end_piece;
            
            // white to move
            if (turn == white)
            {
                start_piece = p;
                end_piece = k;
            }
            
            // black to move
            else
            {
                start_piece = P;
                end_piece = K;
            }
            
            // loop over bitboards opposite to the current side to move
            for (int bb_piece = start_piece; bb_piece <= end_piece; bb_piece++)
            {
                // if there's a piece on the target square
                if (GET_BIT(bitboards[bb_piece], target_square))
                {
                    // remove it from corresponding bitboard
                    RESET_BIT(bitboards[bb_piece], target_square);
                    hashKey ^= pieceKeys[bb_piece][target_square];
                    break;
                }
            }
        }
        
        // handle pawn promotions
        if (promoted_piece)
        {
            // erase the pawn from the target square
            if (turn == white)
            {
                RESET_BIT(bitboards[P], target_square);
                hashKey ^= pieceKeys[P][target_square];
            }
            else
            {
                RESET_BIT(bitboards[p], target_square);
                hashKey ^= pieceKeys[p][target_square];
            }
            // set up promoted piece on chess board
            SET_BIT(bitboards[promoted_piece], target_square);
            hashKey ^= pieceKeys[promoted_piece][target_square];
        }
        
        // handle enpassant captures
        if (enpass)
        {
            fifty = 0;
            // erase the pawn depending on side to move
            (turn == white) ? RESET_BIT(bitboards[p], target_square + 8) :
                              RESET_BIT(bitboards[P], target_square - 8);
                              
            if (turn == white)
            {
                RESET_BIT(bitboards[p], target_square + 8);
                hashKey ^= pieceKeys[p][target_square + 8];
            }
            else
            {
                RESET_BIT(bitboards[P], target_square - 8);
                hashKey ^= pieceKeys[P][target_square - 8];
            }
        }
        
        if (enpassant != noSq) hashKey ^= enpassantKeys[enpassant];

        // reset enpassant square
        enpassant = noSq;

        // handle double pawn push
        if (double_push)
        {
            // set enpassant aquare depending on side to move
            if (turn == white)
            {
                enpassant = target_square + 8;
                hashKey ^= enpassantKeys[target_square + 8];
            }
            else
            {
                enpassant = target_square - 8;
                hashKey ^= enpassantKeys[target_square - 8];

            }
        }
        
        // handle castling moves
        if (castling)
        {
            // switch target square
            switch (target_square)
            {
                // white castles king side
                case (g1):
                    // move H rook
                    RESET_BIT(bitboards[R], h1);
                    SET_BIT(bitboards[R], f1);
                    hashKey ^= pieceKeys[R][h1];
                    hashKey ^= pieceKeys[R][f1];
                    break;
                
                // white castles queen side
                case (c1):
                    // move A rook
                    RESET_BIT(bitboards[R], a1);
                    SET_BIT(bitboards[R], d1);
                    hashKey ^= pieceKeys[R][a1];
                    hashKey ^= pieceKeys[R][d1];
                    break;
                
                // black castles king side
                case (g8):
                    // move H rook
                    RESET_BIT(bitboards[r], h8);
                    SET_BIT(bitboards[r], f8);
                    hashKey ^= pieceKeys[r][h8];
                    hashKey ^= pieceKeys[r][f8];
                    break;
                
                // black castles queen side
                case (c8):
                    // move A rook
                    RESET_BIT(bitboards[r], a8);
                    SET_BIT(bitboards[r], d8);
                    hashKey ^= pieceKeys[r][a8];
                    hashKey ^= pieceKeys[r][d8];
                    break;
            }
        }

        hashKey ^= castleKeys[castle];
        
        // update castling rights
        castle &= castlingRights[source_square];
        castle &= castlingRights[target_square];

        hashKey ^= castleKeys[castle];

        
        // reset occupancies
        memset(occupancies, 0ULL, 24);
        
        // loop over white pieces bitboards
        for (int bb_piece = P; bb_piece <= K; bb_piece++)
            // update white occupancies
            occupancies[white] |= bitboards[bb_piece];

        // loop over black pieces bitboards
        for (int bb_piece = p; bb_piece <= k; bb_piece++)
            // update black occupancies
            occupancies[black] |= bitboards[bb_piece];

        // update both sides occupancies
        occupancies[both] |= occupancies[white];
        occupancies[both] |= occupancies[black];
        
        // change side
        turn ^= 1;
         
        hashKey ^= turnKey;
        
        // make sure that king has not been exposed into a check
        if (isSquareAttacked((turn == white) ? getIdxOfLSB(bitboards[k]) : getIdxOfLSB(bitboards[K]), turn))
        {
            // take move back
            RESTORE_BOARD()
            
            // return illegal move
            return 0;
        }
        
        //
        else
            // return legal move
            return 1;
            
            
    }
    
    // capture moves
    else
    {
        // make sure move is the capture
        if (GET_MOVE_CAPTURE(move))
            makeMove(move, allMoves);
        
        // otherwise the move is not a capture
        else
            // don't make it
            return 0;
    }
}

static inline void generateMoves(moves *moveList)
{
    moveList->counter = 0;
    int sourceSquare, targetSquare;
    U64 bitboard, attacks;

    for (int piece = P; piece <= k; piece++)
    {
        bitboard = bitboards[piece];
        // pawns and kings castling
        if (turn == white)
        {
            // white pawn
            if (piece == P)
            {
                while (bitboard)
                {
                    sourceSquare = getIdxOfLSB(bitboard);
                    targetSquare = sourceSquare - 8;

                    // if target square is empty and inside the board
                    if (!(targetSquare < 0) && !GET_BIT(occupancies[both], targetSquare))
                    {
                        // promotions
                        if (sourceSquare >= a7 && sourceSquare <= h7)
                        {
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, Q, 0, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, R, 0, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, B, 0, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, N, 0, 0, 0, 0));
                        }
                        else
                        {
                            // step forword one square
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 0, 0, 0, 0));


                            // step forword two square
                            if ((sourceSquare >= a2 && sourceSquare <= h2) && !GET_BIT(occupancies[both], targetSquare-8))
                                addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare-8, piece, 0, 0, 1, 0, 0));

                        }
                    }
                    attacks = pawnAttacks[turn][sourceSquare] & occupancies[black];
                    // pawn caputres
                    while (attacks)
                    {
                        targetSquare = getIdxOfLSB(attacks);
                        if (sourceSquare >= a7 && sourceSquare <= h7)
                        {
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, Q, 1, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, R, 1, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, B, 1, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, N, 1, 0, 0, 0));
                        }
                        else
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 1, 0, 0, 0));

                        RESET_BIT(attacks, targetSquare);

                    }

                    // enpassant
                    if (enpassant != noSq)
                    {
                        // pawn attacks an enpassant square
                        U64 enpassantAttacks = pawnAttacks[turn][sourceSquare] & (1Ull << enpassant);
                        if (enpassantAttacks)
                        {
                            int targetEnpassant = getIdxOfLSB(enpassantAttacks);
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetEnpassant, piece, 0, 1, 0, 1, 0));
                        }
                    }
                    RESET_BIT(bitboard, sourceSquare);
                }
            }
            // castling moves
            if (piece == K)
            {
                // king's side castling
                if (castle & wk)
                {
                    // squares between king and king's side rook are empty
                    if (!GET_BIT(occupancies[both], f1) && !GET_BIT(occupancies[both], g1))
                    {
                        // squares king is passing are not under attack
                        if (!isSquareAttacked(e1, black) && !isSquareAttacked(f1, black))
                            addMove(moveList, ENCODE_MOVE(e1, g1, piece, 0, 0, 0, 0, 1));

                    }
                }

                // queen's side castling
                if (castle & wq)
                {
                    // squares between king and queen's side rook are empty
                    if (!GET_BIT(occupancies[both], d1) && !GET_BIT(occupancies[both], c1) && !GET_BIT(occupancies[both], b1))
                    {
                        // squares king is passing are not under attack
                        if (!isSquareAttacked(e1, black) && !isSquareAttacked(d1, black))
                            addMove(moveList, ENCODE_MOVE(e1, c1, piece, 0, 0, 0, 0, 1));


                    }
                }

            }
        }
        else
        {
            // black pawn
            if (piece == p)
            {
                while (bitboard)
                {
                    sourceSquare = getIdxOfLSB(bitboard);
                    targetSquare = sourceSquare + 8;

                    // if target square is empty and inside the board
                    if (!(targetSquare > h1) && !GET_BIT(occupancies[both], targetSquare))
                    {
                        // promotions
                        if (sourceSquare >= a2 && sourceSquare <= h2)
                        {
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, q, 0, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, r, 0, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, b, 0, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, n, 0, 0, 0, 0));
                        }
                        else
                        {
                            // step forword one square
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 0, 0, 0, 0));


                            // step forword two square
                            if ((sourceSquare >= a7 && sourceSquare <= h7) && !GET_BIT(occupancies[both], targetSquare+8))
                                addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare+8, piece, 0, 0, 1, 0, 0));


                        }
                    }
                    attacks = pawnAttacks[turn][sourceSquare] & occupancies[white];
                    // pawn caputres
                    while (attacks)
                    {
                        targetSquare = getIdxOfLSB(attacks);
                        if (sourceSquare >= a2 && sourceSquare <= h2)
                        {
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, q, 1, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, r, 1, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, b, 1, 0, 0, 0));
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, n, 1, 0, 0, 0));
                        }
                        else
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 1, 0, 0, 0));

                        RESET_BIT(attacks, targetSquare);

                    }

                    // enpassant
                    if (enpassant != noSq)
                    {
                        // pawn attacks an enpassant square
                        U64 enpassantAttacks = pawnAttacks[turn][sourceSquare] & (1Ull << enpassant);
                        if (enpassantAttacks)
                        {
                            int targetEnpassant = getIdxOfLSB(enpassantAttacks);
                            addMove(moveList, ENCODE_MOVE(sourceSquare, targetEnpassant, piece, 0, 1, 0, 1, 0));
                        }
                    }

                    RESET_BIT(bitboard, sourceSquare);

                }
            }
            // castling moves
            if (piece == k)
            {
                // king's side castling
                if (castle & bk)
                {
                    // squares between king and king's side rook are empty
                    if (!GET_BIT(occupancies[both], f8) && !GET_BIT(occupancies[both], g8))
                    {
                        // squares king is passing are not under attack
                        if (!isSquareAttacked(e8, white) && !isSquareAttacked(f8, white))
                            addMove(moveList, ENCODE_MOVE(e8, g8, piece, 0, 0, 0, 0, 1));

                    }
                }

                // queen's side castling
                if (castle & bq)
                {
                    // squares between king and queen's side rook are empty
                    if (!GET_BIT(occupancies[both], d8) && !GET_BIT(occupancies[both], c8) && !GET_BIT(occupancies[both], b8))
                    {
                        // squares king is passing are not under attack
                        if (!isSquareAttacked(e8, white) && !isSquareAttacked(d8, white))
                            addMove(moveList, ENCODE_MOVE(e8, c8, piece, 0, 0, 0, 0, 1));


                    }
                }

            }
        }

        // knighs
        if ((turn == white) ? piece == N : piece == n)
        {
            while (bitboard)
            {
                sourceSquare = getIdxOfLSB(bitboard);
                attacks = knightAttacks[sourceSquare] & ((turn == white) ? ~occupancies[white] : ~occupancies[black]);

                while (attacks) 
                {
                    targetSquare = getIdxOfLSB(attacks);
                    // quit move
                    if (!GET_BIT(((turn == white) ? occupancies[black] : occupancies[white]), targetSquare))
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 0, 0, 0, 0));
                        
                    // capture move
                    else
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 1, 0, 0, 0));



                    RESET_BIT(attacks, targetSquare);
                }

                RESET_BIT(bitboard, sourceSquare);
            }
        }
        // bishops
        if ((turn == white) ? piece == B : piece == b)
        {
            while (bitboard)
            {
                sourceSquare = getIdxOfLSB(bitboard);
                attacks = getBishopAttacks(sourceSquare, occupancies[both]) & ((turn == white) ? ~occupancies[white] : ~occupancies[black]);

                while (attacks) 
                {
                    targetSquare = getIdxOfLSB(attacks);
                    // quit move
                    if (!GET_BIT(((turn == white) ? occupancies[black] : occupancies[white]), targetSquare))
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 0, 0, 0, 0));

                    // capture move
                    else
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 1, 0, 0, 0));

                    RESET_BIT(attacks, targetSquare);
                }

                RESET_BIT(bitboard, sourceSquare);
            }
        }
        // rooks
        if ((turn == white) ? piece == R : piece == r)
        {
            while (bitboard)
            {
                sourceSquare = getIdxOfLSB(bitboard);
                attacks = getRookAttacks(sourceSquare, occupancies[both]) & ((turn == white) ? ~occupancies[white] : ~occupancies[black]);

                while (attacks) 
                {
                    targetSquare = getIdxOfLSB(attacks);
                    // quit move
                    if (!GET_BIT(((turn == white) ? occupancies[black] : occupancies[white]), targetSquare))
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 0, 0, 0, 0));
                    // capture move
                    else
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 1, 0, 0, 0));

                    RESET_BIT(attacks, targetSquare);
                }

                RESET_BIT(bitboard, sourceSquare);
            }
        }
        // queens
        if ((turn == white) ? piece == Q : piece == q)
        {
            while (bitboard)
            {
                sourceSquare = getIdxOfLSB(bitboard);
                attacks = getQueenAttacks(sourceSquare, occupancies[both]) & ((turn == white) ? ~occupancies[white] : ~occupancies[black]);

                while (attacks) 
                {
                    targetSquare = getIdxOfLSB(attacks);
                    // quit move
                    if (!GET_BIT(((turn == white) ? occupancies[black] : occupancies[white]), targetSquare))
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 0, 0, 0, 0));
                    // capture move
                    else
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 1, 0, 0, 0));

                    RESET_BIT(attacks, targetSquare);
                }

                RESET_BIT(bitboard, sourceSquare);
            }
        }
        // kings
        if ((turn == white) ? piece == K : piece == k)
        {
            while (bitboard)
            {
                sourceSquare = getIdxOfLSB(bitboard);
                attacks = kingAttacks[sourceSquare] & ((turn == white) ? ~occupancies[white] : ~occupancies[black]);

                while (attacks) 
                {
                    targetSquare = getIdxOfLSB(attacks);
                    // quit move
                    if (!GET_BIT(((turn == white) ? occupancies[black] : occupancies[white]), targetSquare))
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 0, 0, 0, 0));
                    // capture move
                    else
                        addMove(moveList, ENCODE_MOVE(sourceSquare, targetSquare, piece, 0, 1, 0, 0, 0));

                    RESET_BIT(attacks, targetSquare);
                }

                RESET_BIT(bitboard, sourceSquare);
            }
        }
    }
}

//              Performance
// ====================================

U64 nodes;

static inline void perftDriver(int depth)
{
    if (depth == 0)
    {
        nodes++;
        return;
    }

    moves moveList[1];
    generateMoves(moveList);

    for (int moveCount = 0; moveCount < moveList->counter; moveCount++)
    {
        COPY_BOARD();
        if (!makeMove(moveList->moves[moveCount], allMoves))
            continue;
        perftDriver(depth-1);
        RESTORE_BOARD();

        // U64 hashFromScratch = generateHashKey();
        // if (hashKey != hashFromScratch)
        // {
        //     printf("\n\nTake back\n");
        //     printf("move: "); printMove(moveList->moves[moveCount]);
        //     printBoard();
        //     printf("hash key should be: %llx\n", hashFromScratch);
        //     getchar();
        // }
    }
}

void perftTest(int depth)
{
    printf("\n   Performane Test\n\n");

    moves moveList[1];
    generateMoves(moveList);

    long start = getTimeInMillis();

    for (int moveCount = 0; moveCount < moveList->counter; moveCount++)
    {
        COPY_BOARD();
        if (!makeMove(moveList->moves[moveCount], allMoves))
            continue;
        
        long nodesSoFar = nodes;
        perftDriver(depth-1);
        long oldNodes = nodes - nodesSoFar;
        RESTORE_BOARD();
        printf("   move: %s%s%c  nodes: %ld\n", squareToCoord[GET_MOVE_SOURCE(moveList->moves[moveCount])], squareToCoord[GET_MOVE_TARGET(moveList->moves[moveCount])], promotedPieces[GET_MOVE_PROMOTED(moveList->moves[moveCount])], oldNodes);
    }
    printf("\n   Depth: %d\n", depth);
    printf("\n   Nodes: %lld\n", nodes);
    printf("\n   Time: %ldms\n\n", getTimeInMillis()-start);
}

//              Magics
// ====================================
U64 findMagicNumber(int square, int relevantBits, int bishop)
{
    U64 occupancies[4096];
    U64 attacks[4096];
    U64 usedAttacks[4096];
    U64 attackMask = bishop ? maskBishopAttacks(square) : maskRookAttacks(square);
    int occupancyIndicies = 1 << relevantBits;

    for (int idx = 0; idx < occupancyIndicies; idx++)
    {
        occupancies[idx] = setOccupancy(idx, relevantBits, attackMask);
        attacks[idx] = bishop ? bishopAttacksInRunTime(square, occupancies[idx]) : rookAttacksInRunTime(square, occupancies[idx]);
    }

    for (int randomCount = 0; randomCount < 100000000; randomCount++)
    {
        U64 magicNumber = generateMagicNumber();
        if (countOnes((attackMask * magicNumber) & 0xFF00000000000000) < 6) continue;
        memset(usedAttacks, 0ULL, sizeof(usedAttacks));
        int idx, fail;
        for (idx = 0, fail = 0; !fail && idx < occupancyIndicies; idx++)
        {
            int magicIdx = (int)((occupancies[idx] * magicNumber) >> (64-relevantBits));
            if (usedAttacks[magicIdx] == 0ULL)
                usedAttacks[magicIdx] = attacks[idx];
            else if (usedAttacks[magicIdx] != attacks[idx])
                fail = 1;

        }
        if (!fail)
            return magicNumber;
    }
    printf("  Magic numer fails!");
    return 0ULL;
}

void initMagicNumbers()
{
    for (int square = 0; square < 64; square++)
    {
        rookMagicNumbers[square] = findMagicNumber(square, rookRelevantBits[square], rook);
    }

    for (int square = 0; square < 64; square++)
    {
        bishopMagicNumbers[square] = findMagicNumber(square, bishopRelevantBits[square], bishop);

    }
}

//                Engine
// ====================================
const int materialScore[2][12] =
{
    // opening material score
    82, 337, 365, 477, 1025, 12000, -82, -337, -365, -477, -1025, -12000,
    
    // endgame material score
    94, 281, 297, 512,  936, 12000, -94, -281, -297, -512,  -936, -12000
};

// game phase scores
const int opening_phase_score = 6192;
const int endgame_phase_score = 518;
const int ex_endgame_phase_score = 2600;

// game phases
enum { opening, endgame, middlegame };

enum { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING };

const int positionalScore[2][6][64] =

// opening positional piece scores //
{
    //pawn
    0,   0,   0,   0,   0,   0,  0,   0,
    98, 134,  61,  95,  68, 126, 34, -11,
    -6,   7,  26,  31,  65,  56, 25, -20,
    -14,  13,   6,  21,  23,  12, 17, -23,
    -27,  -2,  -5,  12,  17,   6, 10, -25,
    -26,  -4,  -4, -10,   3,   3, 33, -12,
    -35,  -1, -20, -23, -15,  24, 38, -22,
    0,   0,   0,   0,   0,   0,  0,   0,
    
    // knight
    -167, -89, -34, -49,  61, -97, -15, -107,
    -73, -41,  72,  36,  23,  62,   7,  -17,
    -47,  60,  37,  65,  84, 129,  73,   44,
    -9,  17,  19,  53,  37,  69,  18,   22,
    -13,   4,  16,  13,  28,  19,  21,   -8,
    -23,  -9,  12,  10,  19,  17,  25,  -16,
    -29, -53, -12,  -3,  -1,  18, -14,  -19,
    -105, -21, -58, -33, -17, -28, -19,  -23,
    
    // bishop
    -29,   4, -82, -37, -25, -42,   7,  -8,
    -26,  16, -18, -13,  30,  59,  18, -47,
    -16,  37,  43,  40,  35,  50,  37,  -2,
    -4,   5,  19,  50,  37,  37,   7,  -2,
    -6,  13,  13,  26,  34,  12,  10,   4,
    0,  15,  15,  15,  14,  27,  18,  10,
    4,  15,  16,   0,   7,  21,  33,   1,
    -33,  -3, -14, -21, -13, -12, -39, -21,
    
    // rook
    32,  42,  32,  51, 63,  9,  31,  43,
    27,  32,  58,  62, 80, 67,  26,  44,
    -5,  19,  26,  36, 17, 45,  61,  16,
    -24, -11,   7,  26, 24, 35,  -8, -20,
    -36, -26, -12,  -1,  9, -7,   6, -23,
    -45, -25, -16, -17,  3,  0,  -5, -33,
    -44, -16, -20,  -9, -1, 11,  -6, -71,
    -19, -13,   1,  17, 16,  7, -37, -26,
    
    // queen
    -28,   0,  29,  12,  59,  44,  43,  45,
    -24, -39,  -5,   1, -16,  57,  28,  54,
    -13, -17,   7,   8,  29,  56,  47,  57,
    -27, -27, -16, -16,  -1,  17,  -2,   1,
    -9, -26,  -9, -10,  -2,  -4,   3,  -3,
    -14,   2, -11,  -2,  -5,   2,  14,   5,
    -35,  -8,  11,   2,   8,  15,  -3,   1,
    -1, -18,  -9,  10, -15, -25, -31, -50,
    
    // king
    -65,  23,  16, -15, -56, -34,   2,  13,
    29,  -1, -20,  -7,  -8,  -4, -38, -29,
    -9,  24,   2, -16, -20,   6,  22, -22,
    -17, -20, -12, -27, -30, -25, -14, -36,
    -49,  -1, -27, -39, -46, -44, -33, -51,
    -14, -14, -22, -46, -44, -30, -15, -27,
    1,   7,  -8, -64, -43, -16,   9,   8,
    -15,  36,  12, -54,   8, -28,  24,  14,


    // Endgame positional piece scores //

    //pawn
    0,   0,   0,   0,   0,   0,   0,   0,
    178, 173, 158, 134, 147, 132, 165, 187,
    94, 100,  85,  67,  56,  53,  82,  84,
    32,  24,  13,   5,  -2,   4,  17,  17,
    13,   9,  -3,  -7,  -7,  -8,   3,  -1,
    4,   7,  -6,   1,   0,  -5,  -1,  -8,
    13,   8,   8,  10,  13,   0,   2,  -7,
    0,   0,   0,   0,   0,   0,   0,   0,
    
    // knight
    -58, -38, -13, -28, -31, -27, -63, -99,
    -25,  -8, -25,  -2,  -9, -25, -24, -52,
    -24, -20,  10,   9,  -1,  -9, -19, -41,
    -17,   3,  22,  22,  22,  11,   8, -18,
    -18,  -6,  16,  25,  16,  17,   4, -18,
    -23,  -3,  -1,  15,  10,  -3, -20, -22,
    -42, -20, -10,  -5,  -2, -20, -23, -44,
    -29, -51, -23, -15, -22, -18, -50, -64,
    
    // bishop
    -14, -21, -11,  -8, -7,  -9, -17, -24,
    -8,  -4,   7, -12, -3, -13,  -4, -14,
    2,  -8,   0,  -1, -2,   6,   0,   4,
    -3,   9,  12,   9, 14,  10,   3,   2,
    -6,   3,  13,  19,  7,  10,  -3,  -9,
    -12,  -3,   8,  10, 13,   3,  -7, -15,
    -14, -18,  -7,  -1,  4,  -9, -15, -27,
    -23,  -9, -23,  -5, -9, -16,  -5, -17,
    
    // rook
    13, 10, 18, 15, 12,  12,   8,   5,
    11, 13, 13, 11, -3,   3,   8,   3,
    7,  7,  7,  5,  4,  -3,  -5,  -3,
    4,  3, 13,  1,  2,   1,  -1,   2,
    3,  5,  8,  4, -5,  -6,  -8, -11,
    -4,  0, -5, -1, -7, -12,  -8, -16,
    -6, -6,  0,  2, -9,  -9, -11,  -3,
    -9,  2,  3, -1, -5, -13,   4, -20,
    
    // queen
    -9,  22,  22,  27,  27,  19,  10,  20,
    -17,  20,  32,  41,  58,  25,  30,   0,
    -20,   6,   9,  49,  47,  35,  19,   9,
    3,  22,  24,  45,  57,  40,  57,  36,
    -18,  28,  19,  47,  31,  34,  39,  23,
    -16, -27,  15,   6,   9,  17,  10,   5,
    -22, -23, -30, -16, -16, -23, -36, -32,
    -33, -28, -22, -43,  -5, -32, -20, -41,
    
    // king
    -74, -35, -18, -18, -11,  15,   4, -17,
    -12,  17,  14,  17,  17,  38,  23,  11,
    10,  17,  23,  15,  20,  45,  44,  13,
    -8,  22,  24,  27,  26,  33,  26,   3,
    -18,  -4,  21,  24,  27,  23,   9, -11,
    -19,  -3,  11,  21,  23,  16,   7,  -9,
    -27, -11,   4,  13,  14,   4,  -5, -17,
    -53, -34, -21, -11, -28, -14, -24, -43
};

const int mirrorScore[128] =
{
	a1, b1, c1, d1, e1, f1, g1, h1,
	a2, b2, c2, d2, e2, f2, g2, h2,
	a3, b3, c3, d3, e3, f3, g3, h3,
	a4, b4, c4, d4, e4, f4, g4, h4,
	a5, b5, c5, d5, e5, f5, g5, h5,
	a6, b6, c6, d6, e6, f6, g6, h6,
	a7, b7, c7, d7, e7, f7, g7, h7,
	a8, b8, c8, d8, e8, f8, g8, h8
};

U64 fileMasks[64];
U64 rankMasks[64];
U64 isolatedMasks[64];
U64 whitePassedMasks[64];
U64 blackPassedMasks[64];

const int getRank[64] =
{
    7, 7, 7, 7, 7, 7, 7, 7,
    6, 6, 6, 6, 6, 6, 6, 6,
    5, 5, 5, 5, 5, 5, 5, 5,
    4, 4, 4, 4, 4, 4, 4, 4,
    3, 3, 3, 3, 3, 3, 3, 3,
    2, 2, 2, 2, 2, 2, 2, 2,
    1, 1, 1, 1, 1, 1, 1, 1,
	0, 0, 0, 0, 0, 0, 0, 0
};

const int distFromCenter[64] = 
{
    3, 3, 3, 3, 3, 3, 3, 3,
    3, 2, 2, 2, 2, 2, 2, 3,
    3, 2, 1, 1, 1, 1, 2, 3,
    3, 2, 1, 0, 0, 1, 2, 3,
    3, 2, 1, 0, 0, 1, 2, 3,
    3, 2, 1, 1, 1, 1, 2, 3,
    3, 2, 2, 2, 2, 2, 2, 3,
	3, 3, 3, 3, 3, 3, 3, 3
};

int distBetweenKings[64][64];

U64 castledWKS = 7ULL << 61;
U64 castledWQS = 7ULL << 56;

U64 castledBKS = 7ULL << 5;
U64 castledBQS = 7ULL;

U64 blackPawnShieldKS[2] = {7ULL << 13, 7ULL << 21};
U64 blackPawnShieldQS[2] = {7ULL << 8, 7ULL << 16};

U64 whitePawnShieldKS[2] = {7ULL << 53, 7ULL << 45};
U64 whitePawnShieldQS[2] = {7ULL << 48, 7ULL << 40};

int attackWieght[11] = {0, 50, 75, 88, 94, 97, 99, 99, 99, 99};

// pawn structure bonuses and penalties
const int doublePawnPaneltyOpening = -5;
const int doublePawnPaneltyEndgame = -10;

const int isolatedPawnPaneltyOpening = -5;
const int isolatedPawnPaneltyEndgame = -10;

const int passedPawnBonus[8] = {0, 10, 30, 50, 75, 100, 150, 200};
const int candidatePassedPawnBonus[8] = {0, 5, 15, 25, 35, 50, 75, 100};

U64 whiteSupported[64];
U64 blackSupported[64];

U64 phalanx[64];

const int semiOpenFileScore = 10;
const int openFileScore = 15;

static const int bishopUnit = 4;
static const int queenUnit = 9;

static const int bishopMobilityOpening = 5;
static const int bishopMobilityEndgame = 5;

static const int queenMobilityOpening = 5;
static const int queenMobilityEndgame = 5;



const int kingsDefendersBonus = 5;

U64 setFileRankMask(int fileNumber, int rankNumber)
{
    U64 mask = 0ULL;

    for (int rank = 0; rank < 8; rank++)
    {
        for (int file = 0; file < 8; file++)
        {
            int square = rank*8+file;
            if (fileNumber != -1)
            {
                if (file == fileNumber)
                    mask |= SET_BIT(mask, square);
            }
            else if (rankNumber != -1)
            {
                if (rank == rankNumber)
                    mask |= SET_BIT(mask, square);
            }
        }
    }
    
    return mask;
}

void initDistBetweenKings()
{
    int file1, file2, rank1, rank2;
    int rankDistance, fileDistance;
    for (int s1 = 0; s1 < 64; s1++)
    {
        for (int s2 = 0; s2 < 64; s2++)
        {
            file1 = s1 % 8;
            file2 = s2  % 8;
            rank1 = s1 / 8;
            rank2 = s2 / 8;
            rankDistance = abs (rank2 - rank1);
            fileDistance = abs (file2 - file1);
            distBetweenKings[s1][s2] = max(rankDistance, fileDistance);
        }

    }
}

void initConnectedPawns()
{
    for (int rank = 0; rank < 8; rank++)
    {
        for (int file = 0; file < 8; file++)
        {
            int square = rank * 8 + file;
            U64 Wbitboard = 0ULL;
            U64 Bbitboard = 0ULL;
            U64 ph = 0ULL;

            if (file)
            {
                SET_BIT(Wbitboard, square+7);
                SET_BIT(Bbitboard, square-9);
                SET_BIT(ph, square-1);
            }
            if (file != 7)
            {
                SET_BIT(Wbitboard, square+9);
                SET_BIT(Bbitboard, square-7);
                SET_BIT(ph, square+1);
            }
            whiteSupported[square] = Wbitboard;
            blackSupported[square] = Bbitboard;
            phalanx[square] = ph;
        }
    }
}

void initEvaluationMask()
{
    for (int rank = 0; rank < 8; rank++)
    {
        for (int file = 0; file < 8; file++)
        {
            int square = rank*8+file;

            fileMasks[square] = setFileRankMask(file,-1);

            rankMasks[square] = setFileRankMask(-1,rank);

            isolatedMasks[square] |= setFileRankMask(file-1,-1);
            isolatedMasks[square] |= setFileRankMask(file+1,-1);
        }
    }
    for (int rank = 0; rank < 8; rank++)
    {
        for (int file = 0; file < 8; file++)
        {
            int square = rank*8+file;

            whitePassedMasks[square] |= setFileRankMask(file+1,-1);
            whitePassedMasks[square] |= setFileRankMask(file,-1);
            whitePassedMasks[square] |= setFileRankMask(file-1,-1);
            for (int i = 0; i < (8-rank); i++)
            {
                whitePassedMasks[square] &= ~rankMasks[(7-i)*8+file];
            }
            blackPassedMasks[square] |= setFileRankMask(file+1,-1);
            blackPassedMasks[square] |= setFileRankMask(file,-1);
            blackPassedMasks[square] |= setFileRankMask(file-1,-1);
            for (int i = 0; i < rank+1; i++)
                blackPassedMasks[square] &= ~rankMasks[i*8+file];
            

        }
    }
}

static inline int getGamePhaseScore()
{
     int whiteScore = 0, blackScore = 0;

    for (int piece = N; piece <= Q; piece++)
        whiteScore += countOnes(bitboards[piece]) * materialScore[opening][piece];
    for (int piece = n; piece <= q; piece++)
        blackScore += countOnes(bitboards[piece]) * -materialScore[opening][piece];

     return whiteScore+blackScore;
}
int inTheEndGame;

int countPawnIslands(int color)
{
    int pawnIslands = 0;
    int pawns = P;
    if (color == black) pawns = p;
    int sawPawn = 0;
    for (int i = 0; i <= 8; i ++)
    {
        if (i == 8 && sawPawn)
        {
            pawnIslands++;
            break;
        }
        if ((bitboards[pawns] & fileMasks[i]) == 0 && sawPawn) pawnIslands++;
        if (bitboards[pawns] & fileMasks[i]) sawPawn = 1;
        else sawPawn = 0;
    }
    return pawnIslands;
}

int isFaker(int square, int color)
{
    U64 own = bitboards[P];
    U64 against = bitboards[p];
    U64 sides = 0ULL;
    SET_BIT(sides, square+1);
    SET_BIT(sides, square-1);

    U64 rearAttackSpan = blackPassedMasks[square] & ~fileMasks[square] | sides;
    U64 frontAttackSpan = whitePassedMasks[square] & ~fileMasks[square];
    if (color == black)
    {
        rearAttackSpan = whitePassedMasks[square] & ~fileMasks[square] | sides;
        frontAttackSpan = blackPassedMasks[square] & ~fileMasks[square];
        own = bitboards[p];
        against = bitboards[P];
    }
    return (countOnes(rearAttackSpan & own) < countOnes(frontAttackSpan & against));
}

U64 frontSpan(int square, int color)
{
    U64 res = fileMasks[square];
    if (color == black)
        res &= ~((1ULL << square) - 1);
    else
        res &= ((1ULL << square+1) - 1);

    return res;
}

// // convert BBC piece code to Stockfish piece codes
// int nnue_pieces[12] = { 6, 5, 4, 3, 2, 1, 12, 11, 10, 9, 8, 7 };

// // convert BBC square indices to Stockfish indices
// int nnue_squares[64] = {
//     a1, b1, c1, d1, e1, f1, g1, h1,
// 	a2, b2, c2, d2, e2, f2, g2, h2,
// 	a3, b3, c3, d3, e3, f3, g3, h3,
// 	a4, b4, c4, d4, e4, f4, g4, h4,
// 	a5, b5, c5, d5, e5, f5, g5, h5,
// 	a6, b6, c6, d6, e6, f6, g6, h6,
// 	a7, b7, c7, d7, e7, f7, g7, h7,
// 	a8, b8, c8, d8, e8, f8, g8, h8
// };

// // position evaluation
// static inline int evaluate ()
// {
//     // current pieces bitboard copy
//     U64 bitboard;
    
//     // init piece & square
//     int piece, square;
    
//     // array of piece codes converted to Stockfish piece codes
//     int pieces[33];
    
//     // array of square indices converted to Stockfish square indices
//     int squares[33];
    
//     // pieces and squares current index to write next piece square pair at
//     int index = 2;
    
//     // loop over piece bitboards
//     for (int bb_piece = P; bb_piece <= k; bb_piece++)
//     {
//         // init piece bitboard copy
//         bitboard = bitboards[bb_piece];
        
//         // loop over pieces within a bitboard
//         while (bitboard)
//         {
//             // init piece
//             piece = bb_piece;
            
//             // init square
//             square = getIdxOfLSB(bitboard);
            
//             /*
//                 Code to initialize pieces and squares arrays
//                 to serve the purpose of direct probing of NNUE
//             */
            
//             // case white king
//             if (piece == K)
//             {
//                 /* convert white king piece code to stockfish piece code and
//                    store it at the first index of pieces array
//                 */ 
//                 pieces[0] = nnue_pieces[piece];
                
//                 /* convert white king square index to stockfish square index and
//                    store it at the first index of pieces array
//                 */
//                 squares[0] = nnue_squares[square];
//             }
            
//             // case black king
//             else if (piece == k)
//             {
//                 /* convert black king piece code to stockfish piece code and
//                    store it at the second index of pieces array
//                 */
//                 pieces[1] = nnue_pieces[piece];
                
//                 /* convert black king square index to stockfish square index and
//                    store it at the second index of pieces array
//                 */
//                 squares[1] = nnue_squares[square];
//             }
            
//             // all the rest pieces
//             else
//             {
//                 /*  convert all the rest of piece code with corresponding square codes
//                     to stockfish piece codes and square indices respectively
//                 */
//                 pieces[index] = nnue_pieces[piece];
//                 squares[index] = nnue_squares[square];
//                 index++;    
//             }

//             // pop ls1b
//             RESET_BIT(bitboard, square);
//         }
//     }
    
//     // set zero terminating characters at the end of pieces & squares arrays
//     pieces[index] = 0;
//     squares[index] = 0;
    
//     /*
//         We need to make sure that fifty rule move counter gives a penalty
//         to the evaluation, otherwise it won't be capable of mating in
//         simple endgames like KQK or KRK! This expression is used:
//                         nnue_score * (100 - fifty) / 100
//     */
    
//     // get NNUE score (final score! No need to adjust by the side!)
//     return (evaluate_nnue(turn, pieces, squares) * (100 - fifty) / 100);
// }

static inline int evaluate()
{   
    // get game phase score
    int game_phase_score = getGamePhaseScore();
    
    // game phase (opening, middle game, endgame)
    int game_phase = -1;
    
    // pick up game phase based on game phase score
    if (game_phase_score > opening_phase_score) game_phase = opening;
    else if (game_phase_score < endgame_phase_score) game_phase = endgame;
    else game_phase = middlegame;
    
    // static evaluation score
    int score = 0, score_opening = 0, score_endgame = 0;
    
    // current pieces bitboard copy
    U64 bitboard;
    
    // init piece & square
    int piece, square;
    
    // penalties
    int double_pawns = 0;

    int pieceCounter = 0;
    int lethalPieces = 0;

    int blackAttackWeight = 0;
    int blackAttackersCount = 0;

    int whiteAttackWeight = 0;
    int whiteAttackersCount = 0;

    int numberOfSupporter = 0;
    
    // loop over piece bitboards
    for (int bb_piece = P; bb_piece <= k; bb_piece++)
    {
        // init piece bitboard copy
        bitboard = bitboards[bb_piece];
        
        // loop over pieces within a bitboard
        while (bitboard)
        {
            pieceCounter++;
            // init piece
            piece = bb_piece;
            
            // init square
            square = getIdxOfLSB(bitboard);
            
            // get opening/endgame material score
            score_opening += materialScore[opening][piece];
            score_endgame += materialScore[endgame][piece];
            
            // score positional piece scores
            switch (piece)
            {
                // evaluate white pawns
                case P:
                    // get opening/endgame positional score
                    score_opening += positionalScore[opening][PAWN][square];
                    score_endgame += positionalScore[endgame][PAWN][square];

                    // double pawn penalty
                    double_pawns = countOnes(bitboards[P] & fileMasks[square]);
                    
                    // on double pawns (tripple, etc)
                    if (double_pawns > 1)
                    {
                        score_opening += (double_pawns - 1) * doublePawnPaneltyOpening;
                        score_endgame += (double_pawns - 1) * doublePawnPaneltyEndgame;
                    }
                    
                    // on isolated pawn
                    if ((bitboards[P] & isolatedMasks[square]) == 0)
                    {
                        // give an isolated pawn penalty
                        score_opening += isolatedPawnPaneltyOpening;
                        score_endgame += isolatedPawnPaneltyEndgame;
                    }
                    // on passed pawn
                    if ((whitePassedMasks[square] & bitboards[p]) == 0)
                    {
                        // give passed pawn bonus
                        score_opening += passedPawnBonus[getRank[square]];
                        score_endgame += passedPawnBonus[getRank[square]];
                    }
                    // on candidate passed pawn
                    else if ((frontSpan(square, white) & bitboards[p]) == 0)
                    {
                        if (!isFaker(square, white))
                        {
                            score_opening += candidatePassedPawnBonus[getRank[square]];
                            score_endgame += candidatePassedPawnBonus[getRank[square]];
                        }
                    } 

                    numberOfSupporter = countOnes(whiteSupported[square] & bitboards[P]);
                    score_opening += numberOfSupporter*10;
                    score_endgame += numberOfSupporter*10;

                    numberOfSupporter = countOnes(phalanx[square] & bitboards[P]);                  
                    score_opening += numberOfSupporter*7;
                    score_endgame += numberOfSupporter*7;

                    lethalPieces++;
                    break;
                
                // evaluate white knights
                case N:
                    // get opening/endgame positional score
                    score_opening += positionalScore[opening][KNIGHT][square];
                    score_endgame += positionalScore[endgame][KNIGHT][square];

                    if (knightAttacks[square] & blackKingZone[getIdxOfLSB(bitboards[k])])
                    {
                        whiteAttackersCount++;
                        whiteAttackWeight += 20;
                    }
                    
                    break;
                
                // evaluate white bishops
                case B:
                    // get opening/endgame positional score
                    score_opening += positionalScore[opening][BISHOP][square];
                    score_endgame += positionalScore[endgame][BISHOP][square];

                    if (getBishopAttacks(square, occupancies[both]) & blackKingZone[getIdxOfLSB(bitboards[k])])
                    {
                        whiteAttackersCount++;
                        whiteAttackWeight += 20;
                    }
                    
                    // mobility
                    score_opening += (countOnes(getBishopAttacks(square, occupancies[both])) - bishopUnit) * bishopMobilityOpening;
                    score_endgame += (countOnes(getBishopAttacks(square, occupancies[both])) - bishopUnit) * bishopMobilityEndgame;                    
                    break;
                
                // evaluate white rooks
                case R:
                    // get opening/endgame positional score
                    score_opening += positionalScore[opening][ROOK][square];
                    score_endgame += positionalScore[endgame][ROOK][square];

                    if (getRookAttacks(square, occupancies[both]) & blackKingZone[getIdxOfLSB(bitboards[k])])
                    {
                        whiteAttackersCount++;
                        whiteAttackWeight += 40;
                    }

                    // semi open file
                    if ((bitboards[P] & fileMasks[square]) == 0)
                    {
                        // add semi open file bonus
                        score_opening += semiOpenFileScore;
                        score_endgame += semiOpenFileScore;
                    }
                    
                    // semi open file
                    if (((bitboards[P] | bitboards[p]) & fileMasks[square]) == 0)
                    {
                        // add semi open file bonus
                        score_opening += openFileScore;
                        score_endgame += openFileScore;
                    }

                    lethalPieces++;
                    break;
                
                // evaluate white queens
                case Q:
                    // get opening/endgame positional score
                    score_opening += positionalScore[opening][QUEEN][square];
                    score_endgame += positionalScore[endgame][QUEEN][square];

                    if (getQueenAttacks(square, occupancies[both]) & blackKingZone[getIdxOfLSB(bitboards[k])])
                    {
                        whiteAttackersCount++;
                        whiteAttackWeight += 80;
                    }
                    
                    // mobility
                    score_opening += (countOnes(getQueenAttacks(square, occupancies[both])) - queenUnit) * queenMobilityOpening;
                    score_endgame += (countOnes(getQueenAttacks(square, occupancies[both])) - queenUnit) * queenMobilityEndgame;                    

                    lethalPieces++;
                    break;
                // evaluate white king
                case K:
                    // get opening/endgame positional score
                    score_opening += positionalScore[opening][KING][square];
                    score_endgame += positionalScore[endgame][KING][square];
                    
                    // semi open file
                    if ((bitboards[P] & fileMasks[square]) == 0)
                    {
                        // add semi open file penalty
                        score_opening -= semiOpenFileScore;
                        score_endgame -= semiOpenFileScore;
                    }
                    
                    // open file
                    if (((bitboards[P] | bitboards[p]) & fileMasks[square]) == 0)
                    {
                        // add semi open file penalty
                        score_opening -= openFileScore;
                        score_endgame -= openFileScore;
                    }
                    
                    // king safety bonus
                    score_opening += countOnes(kingAttacks[square] & occupancies[white]) * kingsDefendersBonus;
                    score_endgame += countOnes(kingAttacks[square] & occupancies[white]) * kingsDefendersBonus;

                    if (bitboard & castledWKS)
                    {
                        score_opening -= (3-countOnes(bitboards[P]&whitePawnShieldKS[0]))*20;
                        score_endgame -= (3-countOnes(bitboards[P]&whitePawnShieldKS[0]))*20;


                        score_opening += (countOnes(bitboards[P]&whitePawnShieldKS[1]))*15;
                        score_endgame += (countOnes(bitboards[P]&whitePawnShieldKS[1]))*15;
                    }
                    else if (bitboard & castledWQS)
                    {
                        score_opening -= (3-countOnes(bitboards[P]&whitePawnShieldQS[0]))*20;
                        score_endgame -= (3-countOnes(bitboards[P]&whitePawnShieldQS[0]))*20;

                        score_opening += (countOnes(bitboards[P]&whitePawnShieldQS[1]))*15;
                        score_endgame += (countOnes(bitboards[P]&whitePawnShieldQS[1]))*15;
                    }
                    
                    break;

                // evaluate black pawns
                case p:
                    // get opening/endgame positional score
                    score_opening -= positionalScore[opening][PAWN][mirrorScore[square]];
                    score_endgame -= positionalScore[endgame][PAWN][mirrorScore[square]];
                    
                    // double pawn penalty
                    double_pawns = countOnes(bitboards[p] & fileMasks[square]);
                    
                    // on double pawns (tripple, etc)
                    if (double_pawns > 1)
                    {
                        score_opening -= (double_pawns - 1) * doublePawnPaneltyOpening;
                        score_endgame -= (double_pawns - 1) * doublePawnPaneltyEndgame;
                    }
                    
                    // on isolated pawn
                    if ((bitboards[p] & isolatedMasks[square]) == 0)
                    {
                        // give an isolated pawn penalty
                        score_opening -= isolatedPawnPaneltyOpening;
                        score_endgame -= isolatedPawnPaneltyEndgame;
                    }
                    // on passed pawn
                    if ((blackPassedMasks[square] & bitboards[P]) == 0)
                    {
                        // give passed pawn bonus
                        score_opening -= passedPawnBonus[getRank[mirrorScore[square]]];
                        score_endgame -= passedPawnBonus[getRank[mirrorScore[square]]];
                    }
                    // on candidate passed pawn
                    else if ((frontSpan(square, black) & bitboards[P]) == 0)
                    {
                        if (!isFaker(square, black))
                        {
                            score_opening -= candidatePassedPawnBonus[getRank[mirrorScore[square]]];
                            score_endgame -= candidatePassedPawnBonus[getRank[mirrorScore[square]]];
                        }
                    } 

                    numberOfSupporter = countOnes(blackSupported[square] & bitboards[p]);
                    score_opening -= numberOfSupporter*10;
                    score_endgame -= numberOfSupporter*10;

                    numberOfSupporter = countOnes(phalanx[square] & bitboards[p]);                  
                    score_opening -= numberOfSupporter*7;
                    score_endgame -= numberOfSupporter*7;
                    
                    lethalPieces++;
                    break;
                
                // evaluate black knights
                case n:
                    // get opening/endgame positional score
                    score_opening -= positionalScore[opening][KNIGHT][mirrorScore[square]];
                    score_endgame -= positionalScore[endgame][KNIGHT][mirrorScore[square]];

                    if (knightAttacks[square] & whiteKingZone[getIdxOfLSB(bitboards[K])])
                    {
                        blackAttackersCount++;
                        blackAttackWeight += 20;
                    }
                    
                    break;
                
                // evaluate black bishops
                case b:
                    // get opening/endgame positional score
                    score_opening -= positionalScore[opening][BISHOP][mirrorScore[square]];
                    score_endgame -= positionalScore[endgame][BISHOP][mirrorScore[square]];

                    if (getBishopAttacks(square, occupancies[both]) & whiteKingZone[getIdxOfLSB(bitboards[K])])
                    {
                        blackAttackersCount++;
                        blackAttackWeight += 20;
                    }
                    
                    // mobility
                    score_opening -= (countOnes(getBishopAttacks(square, occupancies[both])) - bishopUnit) * bishopMobilityOpening;
                    score_endgame -= (countOnes(getBishopAttacks(square, occupancies[both])) - bishopUnit) * bishopMobilityEndgame;                    
                    break;
                
                // evaluate black rooks
                case r:
                    // get opening/endgame positional score
                    score_opening -= positionalScore[opening][ROOK][mirrorScore[square]];
                    score_endgame -= positionalScore[endgame][ROOK][mirrorScore[square]];

                    if (getRookAttacks(square, occupancies[both]) & whiteKingZone[getIdxOfLSB(bitboards[K])])
                    {
                        blackAttackersCount++;
                        blackAttackWeight += 40;
                    }
                    
                    // semi open file
                    if ((bitboards[p] & fileMasks[square]) == 0)
                    {
                        // add semi open file bonus
                        score_opening -= semiOpenFileScore;
                        score_endgame -= semiOpenFileScore;
                    }
                    
                    // semi open file
                    if (((bitboards[P] | bitboards[p]) & fileMasks[square]) == 0)
                    {    
                        // add semi open file bonus
                        score_opening -= openFileScore;
                        score_endgame -= openFileScore;
                    }
                    
                    lethalPieces++;
                    break;
                
                // evaluate black queens
                case q:
                    // get opening/endgame positional score
                    score_opening -= positionalScore[opening][QUEEN][mirrorScore[square]];
                    score_endgame -= positionalScore[endgame][QUEEN][mirrorScore[square]];

                    if (getQueenAttacks(square, occupancies[both]) & whiteKingZone[getIdxOfLSB(bitboards[K])])
                    {
                        blackAttackersCount++;
                        blackAttackWeight += 80;
                    }
                    
                    // mobility
                    score_opening -= (countOnes(getQueenAttacks(square, occupancies[both])) - queenUnit) * queenMobilityOpening;
                    score_endgame -= (countOnes(getQueenAttacks(square, occupancies[both])) - queenUnit) * queenMobilityEndgame;                    

                    lethalPieces++;
                    break;
                
                // evaluate black king
                case k:
                    // get opening/endgame positional score
                    score_opening -= positionalScore[opening][KING][mirrorScore[square]];
                    score_endgame -= positionalScore[endgame][KING][mirrorScore[square]];
                    
                    // semi open file
                    if ((bitboards[p] & fileMasks[square]) == 0)
                    {
                        // add semi open file penalty
                        score_opening += semiOpenFileScore;
                        score_endgame += semiOpenFileScore;
                    }
                    
                    // semi open file
                    if (((bitboards[P] | bitboards[p]) & fileMasks[square]) == 0)
                    {
                        // add semi open file penalty
                        score_opening += openFileScore;
                        score_endgame += openFileScore;
                    }
                    
                    // king safety bonus
                    score_opening -= countOnes(kingAttacks[square] & occupancies[black]) * kingsDefendersBonus;
                    score_endgame -= countOnes(kingAttacks[square] & occupancies[black]) * kingsDefendersBonus;

                    if (bitboard & castledBKS)
                    {
                        score_opening += (3-countOnes(bitboards[p]&blackPawnShieldKS[0]))*20;
                        score_endgame += (3-countOnes(bitboards[p]&blackPawnShieldKS[0]))*20;

                        score_opening -= (countOnes(bitboards[p]&blackPawnShieldKS[1]))*15;
                        score_endgame -= (countOnes(bitboards[p]&blackPawnShieldKS[1]))*15;
                    }
                    else if (bitboard & castledBQS)
                    {
                        score_opening += (3-countOnes(bitboards[p]&blackPawnShieldQS[0]))*20;
                        score_endgame += (3-countOnes(bitboards[p]&blackPawnShieldQS[0]))*20;

                        score_opening -= (countOnes(bitboards[p]&blackPawnShieldQS[1]))*15;
                        score_endgame -= (countOnes(bitboards[p]&blackPawnShieldQS[1]))*15;
                    }

                    break;
            }

            // pop ls1b
            RESET_BIT(bitboard, square);
        }
    } 

    if (pieceCounter == 2) return 0;
    if (pieceCounter == 3)
        if (!lethalPieces) return 0;

    // interpolate score in the middlegame
    inTheEndGame = 0;
    if (game_phase == middlegame)
        score = (
            score_opening * game_phase_score +
            score_endgame * (opening_phase_score - game_phase_score)
        ) / opening_phase_score;

    // return pure opening score in opening
    else if (game_phase == opening) score = score_opening;
    
    // return pure endgame score in endgame
    else if (game_phase == endgame)
    {
         score = score_endgame;
         inTheEndGame = 1;
    }

    score += (turn == white) ? 15 : -15;
    if (game_phase_score <= ex_endgame_phase_score)
    {
        int dist = 200 / distBetweenKings[getIdxOfLSB(bitboards[K])][getIdxOfLSB(bitboards[k])];
        // white is winning
        if (score > 0)
            score += dist;

        // black is winning
        else if (score < 0)
            score -= dist;

         score += distFromCenter[getIdxOfLSB(bitboards[K])]*20;
         score -= distFromCenter[getIdxOfLSB(bitboards[k])]*20;
    }
    score += whiteAttackWeight * attackWieght[whiteAttackersCount] / 100;
    score -= blackAttackWeight * attackWieght[blackAttackersCount] / 100;

    score -= (countPawnIslands(white) - 1) * 7;
    score += (countPawnIslands(black) - 1) * 7;

    // return final evaluation based on side
    return (turn == white) ? score * (100-fifty)/100 : -score * (100-fifty)/100;
}

// score bounds for mating score
#define INFINITY 50000
#define MATE_VALUE 49000
#define MATE_SCORE 48000


// most valuable victom -> least valuable attacker
static int mvv_lva[12][12] = {
 	105, 205, 305, 405, 505, 605,  105, 205, 305, 405, 505, 605,
	104, 204, 304, 404, 504, 604,  104, 204, 304, 404, 504, 604,
	103, 203, 303, 403, 503, 603,  103, 203, 303, 403, 503, 603,
	102, 202, 302, 402, 502, 602,  102, 202, 302, 402, 502, 602,
	101, 201, 301, 401, 501, 601,  101, 201, 301, 401, 501, 601,
	100, 200, 300, 400, 500, 600,  100, 200, 300, 400, 500, 600,

	105, 205, 305, 405, 505, 605,  105, 205, 305, 405, 505, 605,
	104, 204, 304, 404, 504, 604,  104, 204, 304, 404, 504, 604,
	103, 203, 303, 403, 503, 603,  103, 203, 303, 403, 503, 603,
	102, 202, 302, 402, 502, 602,  102, 202, 302, 402, 502, 602,
	101, 201, 301, 401, 501, 601,  101, 201, 301, 401, 501, 601,
	100, 200, 300, 400, 500, 600,  100, 200, 300, 400, 500, 600
};

#define MAX_PLY 64

int killerMoves[2][MAX_PLY];

int historyMoves[12][64];

int pvLen[MAX_PLY];

int pvTable[MAX_PLY][MAX_PLY];

int followPv, scorePv;
//          Transposition table
// ====================================



// Transposition table hash flags
#define HASH_F_EXACT 0
#define HASH_F_ALPHA 1
#define HASH_F_BETA 2

int hashEntries = 0;

#define NO_HASH_ENTRY 100000

// Transposition table data structure
typedef struct
{
    U64 hashKey;
    int depth;
    int flags;
    int score;
    int bestMove;
} tt;

tt *transpositionTable = NULL;

void clearTranspositionTable()
{
    tt *hashEntry;
    for (hashEntry = transpositionTable; hashEntry < transpositionTable+hashEntries; hashEntry++)
    {
        hashEntry->hashKey = 0;
        hashEntry->depth = 0;
        hashEntry->flags = 0;
        hashEntry->score = 0;
    }
}

void initHashTable(int mb)
{
    int hashSize = 0x100000*mb;
    hashEntries = hashSize/sizeof(tt);
    if (transpositionTable != NULL)
    {
        free(transpositionTable);
    }
    transpositionTable = (tt*)malloc(hashEntries*sizeof(tt));

    if (transpositionTable == NULL)
    {
        initHashTable(mb/2);
    }
    else
    {
        clearTranspositionTable();
    }
}

static inline int readHashEntry(int alpha, int beta, int depth, int * bestMove)
{
    tt *hashEntry = &transpositionTable[hashKey % hashEntries];
    if (hashEntry->hashKey == hashKey)
    {
        if (hashEntry->depth >= depth)
        {

            int score = hashEntry->score;
            if (score < -MATE_SCORE) score += ply;
            if (score > MATE_SCORE) score -= ply;           

            if (hashEntry->flags == HASH_F_EXACT)
                return score;
            if (hashEntry->flags == HASH_F_ALPHA && score <= alpha)
                return alpha;
            if (hashEntry->flags == HASH_F_BETA && score >= beta)
                return beta;
        }

        *bestMove = hashEntry->bestMove;
    }
    return NO_HASH_ENTRY;
}

static inline void writeHashEntry(int score, int depth, int hashFlag, int bestMove)
{
    tt *hashEntry = &transpositionTable[hashKey % hashEntries];

    if (score < -MATE_SCORE) score -= ply;
    if (score > MATE_SCORE) score += ply;

    
    hashEntry->hashKey = hashKey;
    hashEntry->score = score;
    hashEntry->flags = hashFlag;
    hashEntry->depth = depth;
    hashEntry->bestMove = bestMove;
}

static inline void enablePVScoring(moves * moveList)
{
    followPv = 0;

    for (int count = 0; count < moveList->counter; count++)
    {
        if (pvTable[0][ply] == moveList->moves[count])
        {
            scorePv = 1;
            followPv = 1;
        }
    }
}

static inline int scoreMove(int move)
{
    if (scorePv)
    {
        if(pvTable[0][ply] == move)
        {
            scorePv = 0;
            return 20000;
        }
    }

    // capture move
    if (GET_MOVE_CAPTURE(move))
    {
        int targetPiece = P;

        // pick up bitboard piece index ranges depending on side
            int start_piece, end_piece;
            
            // pick side to move
            if (turn == white) { start_piece = p; end_piece = k; }
            else { start_piece = P; end_piece = K; }
            
            // loop over bitboards opposite to the current side to move
            for (int bb_piece = start_piece; bb_piece <= end_piece; bb_piece++)
            {
                // if there's a piece on the target square
                if (GET_BIT(bitboards[bb_piece], GET_MOVE_TARGET(move)))
                {
                    // remove it from corresponding bitboard
                    targetPiece = bb_piece;
                    break;
                }
            }     
        return mvv_lva[GET_MOVE_PIECE(move)][targetPiece] + 10000;
    }
    // quite move
    else
    {
        if (killerMoves[0][ply] == move)
            return 9000;
        else if (killerMoves[1][ply] == move)
            return 8000;
        else
            return historyMoves[GET_MOVE_PIECE(move)][GET_MOVE_TARGET(move)];
    }
    return 0;
}

static inline int sortMoves(moves *moveList, int bestMove)
{
    int moveScores[moveList->counter];
    for (int count = 0; count < moveList->counter; count++)
    {
        if (bestMove == moveList->moves[count])
            moveScores[count] = 30000;
        else
            moveScores[count] = scoreMove(moveList->moves[count]);
        if (moveScores[count] > 0)
        {
            int j = count-1;
            while (j >= 0 && moveScores[j] <= moveScores[count]) j--;

            int tempMove = moveList->moves[count];
            int tempScore = moveScores[count];
            for (int k = count; k > j+1; k--)
            {
                moveList->moves[k] = moveList->moves[k-1];
                moveScores[k] = moveScores[k-1];

            }
            moveList->moves[j+1] = tempMove;
            moveScores[j+1] = tempScore;
        }
    }
}

static inline int isRepetition()
{
    for (int i = 0; i < repetitionIndex; i++)
    {
        if (repetitionTable[i] == hashKey)
            return 1;     
    }

    return 0;
}

static inline int quiescence(int alpha, int beta)
{
    if (!(nodes & 2047))
        communicate();

    nodes++;

    int evaluation = evaluate();

    if (ply > MAX_PLY-1)
        return evaluate();

    // fail-hard beta cutoff
    if (evaluation >= beta)
        // move fails high
        return beta;

    // found a better move
    if (evaluation > alpha)
    {
        // PV node (move)
        alpha = evaluation;
    }
    moves moveList[1];
    generateMoves(moveList);
    sortMoves(moveList, 0);

    for (int count = 0; count < moveList->counter; count++)
    {
        COPY_BOARD();
        ply++;

        repetitionIndex++;
        repetitionTable[repetitionIndex] = hashKey;

        if (makeMove(moveList->moves[count], onlyCaptures) == 0)
        {
            ply--;

            repetitionIndex--;
        
            continue;
        }
        int score = -quiescence(-beta, -alpha);
        ply--;

        repetitionIndex--;

        RESTORE_BOARD();

        if (stopped == 1) return 0;
        // found a better move
        if (score > alpha)
        {
            // PV node (move)
            alpha = score;

            // fail-hard beta cutoff
            if (score >= beta)
                // move fails high
                return beta;
        }
    }
    return alpha;
}

const int fullDepthMoves = 4;
const int reductionLimit = 3;
static inline int negamax(int alpha, int beta, int depth)
{

    pvLen[ply] = ply;

    int score;

    int bestMove = 0;

    int hashFlag = HASH_F_ALPHA;

    if (ply && isRepetition() || fifty >= 100)
        return 0;

    // hack to figure out whether the current move in a PV move or not
    int pvNode = beta-alpha > 1;

    if (ply && (score = readHashEntry(alpha, beta, depth, &bestMove)) != NO_HASH_ENTRY && !pvNode)
        return score;

    if ((nodes & 2047) == 0)
        communicate();

    if (depth == 0)
        return quiescence(alpha, beta);

    if (ply > MAX_PLY-1)
        return evaluate();
    
    nodes++;

    int isInCheck = isSquareAttacked((turn == white) ? getIdxOfLSB(bitboards[K]) : getIdxOfLSB(bitboards[k]), turn ^ 1);

    if (isInCheck) depth++;

    int legalMoves = 0;

    int static_eval = evaluate();
    
    // evaluation pruning / static null move pruning
	if (depth < 3 && !pvNode && !isInCheck && abs(beta - 1) > -INFINITY + 100)
	{   
        // define evaluation margin
		int eval_margin = 120 * depth;
		
		// evaluation margin substracted from static evaluation score fails high
		if (static_eval - eval_margin >= beta)
		    // evaluation margin substracted from static evaluation score
			return static_eval - eval_margin;
	}

    // null move pruning
    if (depth >= 3 && isInCheck == 0 && ply && !inTheEndGame)
    {
        COPY_BOARD();

        ply++;

        repetitionIndex++;
        repetitionTable[repetitionIndex] = hashKey;

        if (enpassant != noSq) hashKey ^= enpassantKeys[enpassant];

        enpassant = noSq;
        // give the opponent another turn
        turn ^= 1;
        hashKey ^= turnKey;

        score = -negamax(-beta, -beta+1, depth-1-2);

        ply--;

        repetitionIndex--;

        RESTORE_BOARD();

        if (stopped) return 0;

        if (score >= beta)
            return beta;
    }

    // razoring
    if (!pvNode && !isInCheck && depth <= 3)
    {
        // get static eval and add first bonus
        score = static_eval + 125;
        
        // define new score
        int newScore;
        
        // static evaluation indicates a fail-low node
        if (score < beta)
        {
            // on depth 1
            if (depth == 1)
            {
                // get quiscence score
                newScore = quiescence(alpha, beta);
                
                // return quiescence score if it's greater then static evaluation score
                return (newScore > score) ? newScore : score;
            }
            
            // add second bonus to static evaluation
            score += 175;
            
            // static evaluation indicates a fail-low node
            if (score < beta && depth <= 2)
            {
                // get quiscence score
                newScore = quiescence(alpha, beta);
                
                // quiescence score indicates fail-low node
                if (newScore < beta)
                    // return quiescence score if it's greater then static evaluation score
                    return (newScore > score) ? newScore : score;
            }
        }
	}

    moves moveList[1];
    generateMoves(moveList);

    // following PV line
    if (followPv)
        enablePVScoring(moveList);

    sortMoves(moveList, bestMove);

    int moveSearched = 0;

    for (int count = 0; count < moveList->counter; count++)
    {
        COPY_BOARD();
        ply++;

        repetitionIndex++;
        repetitionTable[repetitionIndex] = hashKey;

        if (makeMove(moveList->moves[count], allMoves) == 0)
        {
            ply--;

            repetitionIndex--;        

            continue;
        }
        legalMoves++;

        if (moveSearched == 0)
            score = -negamax(-beta, -alpha, depth-1);
        // late move reduction (LMR)
        else
        {
            if (moveSearched >= fullDepthMoves && depth >= reductionLimit && isInCheck == 0
                    && GET_MOVE_CAPTURE(moveList->moves[count]) == 0 && GET_MOVE_PROMOTED(moveList->moves[count]) == 0)
                score = -negamax(-alpha-1, -alpha, depth-2);
            else score = alpha+1;
            // principle variation search (PVS)
            if (score > alpha)
            {
                score = -negamax(-alpha-1, -alpha, depth-1);
                if ((score > alpha) && (score < beta))
                    score = -negamax(-beta, -alpha, depth-1);
            }
        }

        ply--;

        repetitionIndex--;

        RESTORE_BOARD();

        if (stopped == 1) return 0;

        moveSearched++;

        // found a better move
        if (score > alpha)
        {
            hashFlag = HASH_F_EXACT;

            bestMove = moveList->moves[count];

            if (GET_MOVE_CAPTURE(moveList->moves[count]) == 0)
                historyMoves[GET_MOVE_PIECE(moveList->moves[count])][GET_MOVE_TARGET(moveList->moves[count])] += depth;
            // PV node (move)
            alpha = score;

            pvTable[ply][ply] = moveList->moves[count];

            for (int nextPly = ply+1; nextPly < pvLen[ply+1]; nextPly++)
                pvTable[ply][nextPly] = pvTable[ply+1][nextPly];

            pvLen[ply] = pvLen[ply+1];

            // fail-hard beta cutoff
            if (score >= beta)
            {
                writeHashEntry(beta, depth, HASH_F_BETA, bestMove);

                if (GET_MOVE_CAPTURE(moveList->moves[count]) == 0)
                {
                    killerMoves[1][ply] = killerMoves[0][ply];
                    killerMoves[0][ply] = moveList->moves[count];
                }

                // move fails high
                return beta;
            }
        }
    }
    if (legalMoves == 0)
    {
        if (isInCheck)
            return -MATE_VALUE + ply;
        else
            return 0;
    }

    writeHashEntry(alpha, depth, hashFlag, bestMove);
    
    // move fails low
    return alpha;
}

void searchPosition(int depth)
{
    int score = 0;

    nodes = 0;

    stopped = 0;

    followPv = 0;
    scorePv = 0;

    memset(killerMoves, 0, sizeof(killerMoves));
    memset(historyMoves, 0, sizeof(historyMoves));
    memset(pvTable, 0, sizeof(pvTable));
    memset(pvLen, 0, sizeof(pvLen));

    // iterative deepening
    int alpha = -INFINITY;
    int beta = INFINITY;

    int f = 1;
    for (int currDepth = 1; currDepth <= depth; currDepth++)
    {
        if (stopped == 1) break;

        followPv = 1;
        score = negamax(alpha, beta, currDepth);
        if (score <= alpha || score >= beta)
        {
            alpha = -INFINITY;
            beta = INFINITY;
            currDepth--;
            continue;
        }
        alpha = score - 50;
        beta = score + 50;
        if (pvLen[0])
        {
            if (score > -MATE_VALUE && score < -MATE_SCORE)
            {
                printf("info score mate %d depth %d nodes %lld pv ", -(score + MATE_VALUE) / 2, currDepth, nodes);
                // f = 0;
            }
            else if (score > MATE_SCORE && score < MATE_VALUE)
            {
                printf("info score mate %d depth %d nodes %lld pv ", (MATE_VALUE - score) / 2, currDepth, nodes);
                f = 0;
            }
            else
                printf("info score cp %d depth %d nodes %lld pv ", score, currDepth, nodes);

            for (int count = 0; count < pvLen[0]; count++)
            {
                printMove(pvTable[0][count]);
                printf(" ");
            }
            printf("\n");
        }
        if (!f) break;
    }
    printf("bestmove ");
    printMove(pvTable[0][0]);
    printf("\n"); // 
}


void printMoveScores(moves *moveList)
{
    printf("      Move scores:\n\n");
        for (int count = 0; count < moveList->counter; count++)
        {
            printf("   move: ");
            printMove(moveList->moves[count]);
            printf(" score: %d\n", scoreMove(moveList->moves[count]));
        }
}

//               UCI
// ====================================
// parse user/GUI move string input (e.g. "e7e8q")
int parse_move(char *move_string)
{
    // create move list instance
    moves move_list[1];
    
    // generate moves
    generateMoves(move_list);
    
    // parse source square
    int source_square = (move_string[0] - 'a') + (8 - (move_string[1] - '0')) * 8;
    
    // parse target square
    int target_square = (move_string[2] - 'a') + (8 - (move_string[3] - '0')) * 8;
    
    // loop over the moves within a move list
    for (int move_count = 0; move_count < move_list->counter; move_count++)
    {
        // init move
        int move = move_list->moves[move_count];
        
        // make sure source & target squares are available within the generated move
        if (source_square == GET_MOVE_SOURCE(move) && target_square == GET_MOVE_TARGET(move))
        {
            // init promoted piece
            int promoted_piece = GET_MOVE_PROMOTED(move);
            
            // promoted piece is available
            if (promoted_piece)
            {
                // promoted to queen
                if ((promoted_piece == Q || promoted_piece == q) && move_string[4] == 'q')
                    // return legal move
                    return move;
                
                // promoted to rook
                else if ((promoted_piece == R || promoted_piece == r) && move_string[4] == 'r')
                    // return legal move
                    return move;
                
                // promoted to bishop
                else if ((promoted_piece == B || promoted_piece == b) && move_string[4] == 'b')
                    // return legal move
                    return move;
                
                // promoted to knight
                else if ((promoted_piece == N || promoted_piece == n) && move_string[4] == 'n')
                    // return legal move
                    return move;
                
                // continue the loop on possible wrong promotions (e.g. "e7e8f")
                continue;
            }
            
            // return legal move
            return move;
        }
    }
    
    // return illegal move
    return 0;
}

// parse UCI "position" command
void parse_position(char *command)
{
    // shift pointer to the right where next token begins
    command += 9;
    
    // init pointer to the current character in the command string
    char *current_char = command;
    
    // parse UCI "startpos" command
    if (strncmp(command, "startpos", 8) == 0)
        // init chess board with start position
        parseFEN(start_position);
    
    // parse UCI "fen" command 
    else
    {
        // make sure "fen" command is available within command string
        current_char = strstr(command, "fen");
        
        // if no "fen" command is available within command string
        if (current_char == NULL)
            // init chess board with start position
            parseFEN(start_position);
            
        // found "fen" substring
        else
        {
            // shift pointer to the right where next token begins
            current_char += 4;
            
            // init chess board with position from FEN string
            parseFEN(current_char);
        }
    }
    
    // parse moves after position
    current_char = strstr(command, "moves");
    
    // moves available
    if (current_char != NULL)
    {
        // shift pointer to the right where next token begins
        current_char += 6;
        
        // loop over moves within a move string
        while(*current_char)
        {
            // parse next move
            int move = parse_move(current_char);
            
            // if no more moves
            if (move == 0)
                // break out of the loop
                break;
            
            // increment repetition index
            repetitionIndex++;
            
            // wtire hash key into a repetition table
            repetitionTable[repetitionIndex] = hashKey;
            
            // make move on the chess board
            makeMove(move, allMoves);
            
            // move current character mointer to the end of current move
            while (*current_char && *current_char != ' ') current_char++;
            
            // go to the next move
            current_char++;
        }        
    }
    // printf("readyok\n");
    // print board
    // printBoard();
}

// reset time control variables
void reset_time_control()
{
    // reset timing
    quit = 0;
    movestogo = 30;
    movetime = -1;
    time = -1;
    inc = 0;
    starttime = 0;
    stoptime = 0;
    timeset = 0;
    stopped = 0;
}

// parse UCI command "go"
void parse_go(char *command)
{
    // reset time control
    reset_time_control();
    
    // init parameters
    int depth = -1;

    // init argument
    char *argument = NULL;

    // infinite search
    if ((argument = strstr(command,"infinite"))) {}

    // match UCI "binc" command
    if ((argument = strstr(command,"binc")) && turn == black)
        // parse black time increment
        inc = atoi(argument + 5);

    // match UCI "winc" command
    if ((argument = strstr(command,"winc")) && turn == white)
        // parse white time increment
        inc = atoi(argument + 5);

    // match UCI "wtime" command
    if ((argument = strstr(command,"wtime")) && turn == white)
        // parse white time limit
        time = atoi(argument + 6);

    // match UCI "btime" command
    if ((argument = strstr(command,"btime")) && turn == black)
        // parse black time limit
        time = atoi(argument + 6);

    // match UCI "movestogo" command
    if ((argument = strstr(command,"movestogo")))
        // parse number of moves to go
        movestogo = atoi(argument + 10);

    // match UCI "movetime" command
    if ((argument = strstr(command,"movetime")))
        // parse amount of time allowed to spend to make a move
        movetime = atoi(argument + 9);

    // match UCI "depth" command
    if ((argument = strstr(command,"depth")))
        // parse search depth
        depth = atoi(argument + 6);

    // if move time is not available
    if(movetime != -1)
    {
        // set time equal to move time
        time = movetime;

        // set moves to go to 1
        movestogo = 1;
    }

    // init start time
    starttime = getTimeInMillis();

    // init search depth
    depth = depth;

    // if time control is available
    if(time != -1)
    {
        // flag we're playing with time control
        timeset = 1;

        // set up timing
        time /= movestogo;
        
        // disable time buffer when time is almost up
        if (time > 1500) time -= 50;
        
        // init stoptime
        stoptime = starttime + time + inc;
        
        // treat increment as seconds per move when time is almost up
        if (time < 1500 && inc && depth == 64) stoptime = starttime + inc - 50;
    }

    // if depth is not available
    if(depth == -1)
        // set depth to 64 plies (takes ages to complete...)
        depth = 64;

    // print debug info
    printf("time: %d  start: %u  stop: %u  depth: %d  timeset:%d\n",
            time, starttime, stoptime, depth, timeset);

    // search position
    searchPosition(depth);
}

// main UCI loop
void uci_loop()
{
    // max hash MB
    int max_hash = 128;
    
    // default MB value
    int mb = 64;

    // reset STDIN & STDOUT buffers
    setbuf(stdin, NULL);
    setbuf(stdout, NULL);
    
    // define user / GUI input buffer
    char input[2000];
    
    // main loop
    while (1)
    {
        // reset user /GUI input
        memset(input, 0, sizeof(input));
        
        // make sure output reaches the GUI
        fflush(stdout);
        
        // get user / GUI input
        if (!fgets(input, 2000, stdin))
            // continue the loop
            continue;
        
        // make sure input is available
        if (input[0] == '\n')
            // continue the loop
            continue;
        
        // parse UCI "isready" command
        if (strncmp(input, "isready", 7) == 0)
        {
            printf("readyok\n");
            continue;
        }
        
        // parse UCI "position" command
        else if (strncmp(input, "position", 8) == 0)
        {
            // call parse position function
            parse_position(input);
        
            // clear hash table
            clearTranspositionTable();
        }
        // parse UCI "ucinewgame" command
        else if (strncmp(input, "ucinewgame", 10) == 0)
        {
            // call parse position function
            parse_position("position startpos");
            
            // clear hash table
            clearTranspositionTable();
        }
        // parse UCI "go" command
        else if (strncmp(input, "go", 2) == 0)
            // call parse go function
            parse_go(input);
        
        // parse UCI "quit" command
        else if (strncmp(input, "quit", 4) == 0)
            // quit from the UCI loop (terminate program)
            break;
        
        // parse UCI "uci" command
        else if (strncmp(input, "uci", 3) == 0)
        {
            // print engine info
            printf("id name TTT \n");
            printf("id author Tal Aloni\n");
            printf("uciok\n");
        }

        else if (strncmp(input, "show", 4) == 0)
        {
            printBoard();
        }
        
        else if (!strncmp(input, "setoption name Hash value ", 26)) {			
            // init MB
            sscanf(input,"%*s %*s %*s %*s %d", &mb);
            
            // adjust MB if going beyond the aloowed bounds
            if(mb < 4) mb = 4;
            if(mb > max_hash) mb = max_hash;
            
            // set hash table size in MB
            printf("    Set hash table size to %dMB\n", mb);
            initHashTable(mb);
        }
    }
}

//                Main
// ====================================

void initAll()
{
    initLeapersAttacks();
    initSlidersAttacks(bishop);
    initSlidersAttacks(rook);
    initRandomKeys();
    initHashTable(12);
    initEvaluationMask();
    initDistBetweenKings();
    initConnectedPawns();
    // init_nnue("nn-eba324f53044.nnue");
    // initMagicNumbers();
}

int main()
{
    initAll();

    int debug = 0;
    if (debug)
    {
        // parseFEN("5k2/6p1/8/4NPPP/4n2K/8/8/8 w - - 1 76");
        parseFEN(start_position);

        printBoard();
        printf("score: %d\n", evaluate());
        searchPosition(15);

        free(transpositionTable);
    }
    else
    {
        uci_loop();
        free(transpositionTable);
    }
    return 0;
}