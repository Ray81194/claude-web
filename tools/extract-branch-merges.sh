#!/usr/bin/env bash
# 特定ブランチ（例: 修正1）由来のマージだけを抽出して整理表示する。
#
#   使い方: ./extract-branch-merges.sh [--verify] <対象ブランチ> [統合先ブランチ]
#   例:     ./extract-branch-merges.sh 修正1 main
#           ./extract-branch-merges.sh --verify 修正1 main
#
#   引数1 (必須): 対象ブランチ TOPIC（抽出したいブランチ。例: 修正1）
#   引数2 (任意): 統合先ブランチ BASE（既定: main）
#   --verify    : 混入ゼロの恒等チェックを実行（マージ数に比例して重いので既定off）
#
#   ブランチ名の代わりにコミットSHA・タグ・origin/xxx なども指定可。
#   終了コード: 0=正常 / 1=ブランチが見つからない
#
# 読み取り専用。checkout / merge / rebase / reset は一切実行しない。
set -uo pipefail

VERIFY=0
ARGS=()
for a in "$@"; do
  case "$a" in
    --verify) VERIFY=1 ;;
    -h|--help) sed -n '2,17p' "$0"; exit 0 ;;
    *) ARGS+=("$a") ;;
  esac
done
set -- "${ARGS[@]:-}"

TOPIC="${1:?対象ブランチ名を指定してください（例: 修正1）}"
BASE="${2:-main}"

git rev-parse --verify --quiet "$TOPIC^{commit}" >/dev/null \
  || echo "注意: '$TOPIC' がコミットとして解決できません。" >&2

git rev-parse --verify --quiet "$BASE" >/dev/null || { echo "ブランチが見つかりません: $BASE" >&2; exit 1; }

# --- 1. BASE 上のマージコミットのうち、TOPIC 由来のものを特定 -----------------
# 重要: `git rev-list --merges $BASE` だけでは不十分。
#   「main → 修正1」方向のマージも、修正1 が main に戻された時点で main から
#   到達可能になり一覧に混ざる。その向きのマージは M^1 が修正1側・M^2 が main側
#   と役割が逆転するため、M^1..M^2 が main のコミットを吐いてしまう。
# → --first-parent を付けて「BASE 上で実際に起きたマージ」だけに限定する。
#   main→修正1 のマージは修正1 ブランチ上で起きたものなので、これで除外される。
mapfile -t ALL_MERGES < <(git rev-list --first-parent --merges "$BASE")

MERGES=()
if git rev-parse --verify --quiet "$TOPIC" >/dev/null; then
  # 判定は「M^2 が TOPIC の first-parent 直線上にあるか」で行う。
  # 単なる祖先判定では不十分: TOPIC が BASE から分岐した時点より前に
  # BASE へ入ったマージは全て「TOPIC の祖先」になってしまい、無関係な
  # ブランチのマージまで拾ってしまう。
  # TOPIC を BASE に統合したマージの M^2 は、その時点の TOPIC 先端＝
  # TOPIC の first-parent 直線上の点なので、これで正確に絞れる。
  TFP=$(mktemp); trap 'rm -f "$TFP"' EXIT
  git rev-list --first-parent "$TOPIC" | sort -u > "$TFP"
  for M in "${ALL_MERGES[@]}"; do
    P2=$(git rev-parse --verify --quiet "${M}^2") || continue
    if grep -qx "$P2" "$TFP"; then
      MERGES+=("$M")
    fi
  done
else
  echo "注意: ブランチ '$TOPIC' は存在しません（削除済み？）。メッセージ照合にフォールバックします。" >&2
  mapfile -t MERGES < <(git rev-list --first-parent --merges --grep="$TOPIC" "$BASE")
fi

if [ "${#MERGES[@]}" -eq 0 ]; then
  # 0件でも exit せず、下の「取りこぼしチェック」で ff / squash を判定する。
  echo "###############################################################"
  echo "# 「$BASE」上に「$TOPIC」由来のマージコミットはありません（0 件）"
  echo "###############################################################"
else
  # 古い順に並べ替えて読みやすくする
  mapfile -t MERGES < <(printf '%s\n' "${MERGES[@]}" | tac)

  echo "###############################################################"
  echo "# $TOPIC → $BASE のマージ: ${#MERGES[@]} 件"
  echo "###############################################################"
fi

i=0
for M in "${MERGES[@]}"; do
  i=$((i + 1))
  P1=$(git rev-parse --short "${M}^1")
  P2=$(git rev-parse --short "${M}^2")

  echo
  echo "==============================================================="
  echo "## マージ #${i}: $(git log -1 --format='%h %s' "$M")"
  echo "   日時 : $(git log -1 --format='%ad' --date=iso "$M")"
  echo "   M^1 (マージ直前の $BASE) : $P1"
  echo "   M^2 ($TOPIC の先端)      : $P2"
  echo "==============================================================="

  echo
  echo "--- コミット一覧 (${TOPIC} 由来のみ / ${BASE} 由来は自動除外) ---"
  # M^1..M^2 = 「M^2 から辿れて M^1 から辿れない」コミット。
  # main から修正1 へ取り込んだ分は M^1 から辿れるため、ここで自動的に落ちる。
  git log --oneline --no-merges "${M}^1..${M}^2"

  echo
  echo "--- 検証: ${BASE} 由来の混入がないこと ---"
  # (a) 向きの検証: このマージが確かに TOPIC → BASE 方向であること。
  #     (--first-parent で絞ってあるが、明示的に再確認する)
  if git merge-base --is-ancestor "${M}^2" "$TOPIC" 2>/dev/null; then
    echo "OK: 向き = ${TOPIC} → ${BASE}（M^2 は ${TOPIC} の歴史内）"
  else
    echo "警告: M^2 が ${TOPIC} の歴史にありません。別ブランチのマージの可能性。"
  fi
  # (b) 混入がゼロなのは M^1..M^2 の定義（M^1 の祖先は全て除外）による恒等チェック。
  #     マージ毎に履歴をフル走査するためマージ数に比例して重い。既定ではスキップ。
  if [ "$VERIFY" -eq 1 ]; then
    LEAK=$(comm -12 <(git rev-list --no-merges "${M}^1..${M}^2" | sort -u) \
                    <(git rev-list "${M}^1" | sort -u) | wc -l)
    echo "OK: 範囲内で ${BASE} 側にも存在するコミット = ${LEAK} 件（M^1..M^2 の定義上ゼロ）"
  fi
  # (c) この区間で「main から修正1 へ取り込んだ」ため除外されたコミット数を提示。
  FORK=$(git merge-base "${M}^1" "${M}^2")
  EXCL=$(git rev-list --count --no-merges "${FORK}..${M}^1")
  echo "参考: ${BASE} 側にのみ存在し除外されたコミット = ${EXCL} 件"
  if [ "$EXCL" -gt 0 ]; then
    git log --oneline --no-merges "${FORK}..${M}^1" | sed 's/^/       - /'
  fi

  echo
  echo "--- 差分の統計 (git diff M^1 M) ---"
  git diff --stat "${M}^1" "$M"
done

# --- 3. 取りこぼしの検出 -------------------------------------------------------
echo
echo "==============================================================="
echo "## 取りこぼしチェック"
echo "==============================================================="
if git rev-parse --verify --quiet "$TOPIC" >/dev/null; then
  COVERED=$(mktemp); trap 'rm -f "$TFP" "$COVERED"' EXIT
  for M in "${MERGES[@]}"; do
    git rev-list --no-merges "${M}^1..${M}^2"
  done | sort -u > "$COVERED"

  # (1) TOPIC にあって BASE に無いコミット。
  #     git cherry で「同一パッチが BASE に既に存在するか」まで判定する。
  #       '+' = BASE に相当するパッチが無い（本当に未マージ）
  #       '-' = BASE に同一パッチあり（cherry-pick / rebase 経由で入っている）
  UNMERGED=$(git rev-list --no-merges "${BASE}..${TOPIC}" | sort -u)
  if [ -n "$UNMERGED" ]; then
    echo "・$TOPIC にあるが $BASE に（そのハッシュでは）存在しないコミット:"
    git cherry "$BASE" "$TOPIC" 2>/dev/null | while read -r sign c; do
      if [ "$sign" = "-" ]; then
        echo "   [cherry-pick/rebase 済] $(git log -1 --oneline "$c")"
      else
        echo "   [未マージ or squash 済] $(git log -1 --oneline "$c")"
      fi
    done
    echo "   ※ squash で取り込まれた場合もパッチIDが一致せず [未マージ or squash 済] と出ます。"
  else
    echo "・未マージのコミットはありません。"
  fi

  # (2) fast-forward 検出。
  #     断定できるのは「TOPIC が BASE に完全に含まれるのにマージコミットが無い」場合だけ。
  #     それ以外で ff を事後判定するのは原理的に不可能:
  #     ff で入ったコミットは BASE の first-parent 直線に直接乗るが、
  #     BASE 上で書かれ後に TOPIC へ取り込まれたコミットと形が全く同じになるため。
  #     推測で候補を並べても誤検出だらけになるので、ここでは断定できる場合のみ報告する。
  if [ "${#MERGES[@]}" -eq 0 ] && git merge-base --is-ancestor "$TOPIC" "$BASE" 2>/dev/null; then
    echo "・[検出] $TOPIC は $BASE に完全に含まれるのにマージコミットが0件です。"
    echo "  → fast-forward（または rebase）で取り込まれています。"
    echo "  → M^1..M^2 では復元できません。代わりに以下で概ね確認できます:"
    echo "      git log --oneline \$(git merge-base $BASE $TOPIC)..$TOPIC"
  else
    # 「BASE に到達済み」= BASE から辿れる集合との積。per-commit の merge-base は使わない。
    UNACCOUNTED=$(comm -12 <(git rev-list --no-merges "$TOPIC" | sort -u) \
                           <(git rev-list "$BASE" | sort -u) \
                  | comm -23 - "$COVERED" | wc -l)
    echo "・明確な fast-forward は検出されませんでした。"
    echo "  （$BASE に到達済みでマージ範囲に現れないコミット: ${UNACCOUNTED} 件。"
    echo "    これは ff 由来と $BASE 上で書かれたコミットの両方を含み、事後的に区別できません）"
  fi
  echo
  echo "※ squash マージは元コミットが別ハッシュの1コミットに潰れるため、"
  echo "   構造上ここでは復元できません。git log --oneline $BASE で該当の"
  echo "   squash コミットを目視確認してください。"
else
  echo "ブランチ '$TOPIC' が無いため、取りこぼしチェックはスキップしました。"
fi
